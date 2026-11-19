package br.pucpr.authserver.exceptions

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.beans.TypeMismatchException
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import tools.jackson.databind.DatabindException
import tools.jackson.databind.exc.InvalidFormatException

/**
 * Erros no formato ProblemDetail (RFC 9457), com o campo extra `errors` nas validações.
 * Também devolve `message` (igual ao `detail`), que a tela web e os clientes já leem.
 *
 * - Exceções de domínio anotadas com @ResponseStatus mantêm o status delas (400/401/403/404).
 * - Qualquer outra exceção vira 500 sem detalhes para o cliente; o stacktrace só vai para o log.
 */
@RestControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors = linkedMapOf<String, MutableList<String>>()
        ex.bindingResult.fieldErrors.forEach {
            errors.getOrPut(it.field) { mutableListOf() }.add(it.defaultMessage ?: "invalid value")
        }
        ex.bindingResult.globalErrors.forEach {
            errors.getOrPut("body") { mutableListOf() }.add(it.defaultMessage ?: "invalid value")
        }
        val unprocessable = HttpStatus.UNPROCESSABLE_CONTENT
        return handleExceptionInternal(ex, problem(unprocessable, "Invalid request body", errors), headers, unprocessable, request)
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val cause = ex.cause
        val errors = if (cause is DatabindException) {
            val field = cause.path.joinToString(".") { it.propertyName ?: "[${it.index}]" }.ifBlank { "body" }
            val message = if (cause is InvalidFormatException) "Invalid type. Expected ${cause.targetType.simpleName}"
            else "Invalid field format"
            mapOf(field to listOf(message))
        } else {
            mapOf("body" to listOf("Malformed JSON request"))
        }
        return handleExceptionInternal(ex, problem(HttpStatus.BAD_REQUEST, "Invalid JSON request", errors), headers, status, request)
    }

    override fun handleTypeMismatch(
        ex: TypeMismatchException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors = if (ex is MethodArgumentTypeMismatchException) {
            mapOf(ex.name to listOf("Invalid type. Expected ${ex.requiredType?.simpleName}"))
        } else {
            mapOf("parameter" to listOf("Invalid type"))
        }
        return handleExceptionInternal(ex, problem(HttpStatus.BAD_REQUEST, "Invalid URL parameter", errors), headers, status, request)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException, request: WebRequest): ResponseEntity<Any>? {
        val errors = ex.constraintViolations
            .groupBy({ it.propertyPath.toString().substringAfterLast('.') }, { it.message })
        val unprocessable = HttpStatus.UNPROCESSABLE_CONTENT
        return handleExceptionInternal(ex, problem(unprocessable, "Validation failed", errors), HttpHeaders(), unprocessable, request)
    }

    @ExceptionHandler(Exception::class)
    fun handleAny(ex: Exception, request: WebRequest): ResponseEntity<Any>? {
        // Quem trata segurança é o Spring Security (filtros); aqui só deixamos passar.
        if (ex is AccessDeniedException || ex is AuthenticationException) throw ex

        val annotated = AnnotatedElementUtils.findMergedAnnotation(ex.javaClass, ResponseStatus::class.java)
        if (annotated != null) {
            val status = HttpStatus.valueOf(annotated.code.value())
            val detail = ex.message ?: status.reasonPhrase
            return handleExceptionInternal(ex, problem(status, detail), HttpHeaders(), status, request)
        }

        log.error("Unexpected error", ex)
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        return handleExceptionInternal(ex, problem(status, "Internal server error"), HttpHeaders(), status, request)
    }

    private fun problem(status: HttpStatus, detail: String, errors: Map<String, List<String>>? = null) =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            setProperty("message", detail)
            if (errors != null) setProperty("errors", errors)
        }

    companion object {
        val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    }
}
