package br.pucpr.authserver.exceptions

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.mock.web.MockHttpServletRequest

class ApiExceptionHandlerTest {
    private val handler = ApiExceptionHandler()
    private val request = ServletWebRequest(MockHttpServletRequest())

    @Test
    fun `unknown exceptions become a generic 500 without leaking details`() {
        val response = handler.handleAny(IllegalStateException("senha do banco: 123"), request)!!

        response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        val body = response.body as ProblemDetail
        body.detail shouldBe "Internal server error"
    }

    @Test
    fun `exceptions annotated with ResponseStatus keep their status`() {
        val response = handler.handleAny(BadRequestException("campo invalido"), request)!!

        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        (response.body as ProblemDetail).detail shouldBe "campo invalido"
    }

    @Test
    fun `security exceptions are not swallowed`() {
        shouldThrow<AccessDeniedException> {
            handler.handleAny(AccessDeniedException("negado"), request)
        }
    }
}
