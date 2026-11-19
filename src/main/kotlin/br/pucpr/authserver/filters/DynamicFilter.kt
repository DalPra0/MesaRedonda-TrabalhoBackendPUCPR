package br.pucpr.authserver.filters

import br.pucpr.authserver.exceptions.BadRequestException
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import java.time.Instant

/**
 * Filtro dinâmico: quem chama a API escolhe os critérios pela query string, sem rota nova.
 *
 *     ?durationMinutes.lte=60&weight.gte=2&name.like=cat&status=OPEN
 *
 * Cada parâmetro é `campo` (igual) ou `campo.operador`, com operador em eq, ne, gt, gte, lt, lte, like.
 * Todos os critérios são combinados com AND. Só entram campos da lista `fields` de cada entidade
 * (nada de filtrar por coluna arbitrária) e valor ou operador inválido devolve 400.
 */
object DynamicFilter {
    private val ORDERING = setOf("gt", "gte", "lt", "lte")
    private val OPERATORS = setOf("eq", "ne", "like") + ORDERING

    private class Criterion(val field: String, val op: String, val value: Any)

    /** `ignore` são os parâmetros que o endpoint já trata por conta própria (ex.: `sort`). */
    fun <T : Any> of(
        params: Map<String, String>,
        fields: Map<String, Class<*>>,
        ignore: Set<String> = emptySet(),
    ): Specification<T> {
        val criteria = params
            .filter { (key, value) -> key !in ignore && value.isNotBlank() }
            .map { (key, value) -> parse(key, value.trim(), fields) }

        return Specification { root, _, cb ->
            cb.and(*criteria.map { toPredicate(it, root, cb) }.toTypedArray())
        }
    }

    private fun parse(key: String, raw: String, fields: Map<String, Class<*>>): Criterion {
        val field = key.substringBefore('.')
        val op = if ('.' in key) key.substringAfter('.') else "eq"
        val type = fields[field] ?: throw BadRequestException("Invalid filter field: $field")
        if (op !in OPERATORS) throw BadRequestException("Invalid filter operator: $op")
        if (op == "like" && type != String::class.java) {
            throw BadRequestException("Operator like only works on text fields: $field")
        }
        if (op in ORDERING && (type == String::class.java || type == Boolean::class.javaObjectType || type.isEnum)) {
            throw BadRequestException("Operator $op does not work on field: $field")
        }
        return Criterion(field, op, convert(field, raw, type))
    }

    private fun convert(field: String, raw: String, type: Class<*>): Any = try {
        when {
            type == String::class.java -> raw
            type == Int::class.javaObjectType -> raw.toInt()
            type == Long::class.javaObjectType -> raw.toLong()
            type == Boolean::class.javaObjectType -> raw.toBooleanStrict()
            type == Instant::class.java -> Instant.parse(raw)
            type.isEnum -> type.enumConstants.first { (it as Enum<*>).name.equals(raw, ignoreCase = true) }
            else -> throw IllegalArgumentException("unsupported type")
        }
    } catch (_: Exception) {
        throw BadRequestException("Invalid value for filter $field: $raw")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> toPredicate(c: Criterion, root: Root<T>, cb: CriteriaBuilder): Predicate {
        if (c.op == "like") {
            return cb.like(cb.lower(root.get<String>(c.field)), "%${(c.value as String).lowercase()}%")
        }
        val path = root.get<Comparable<Any>>(c.field)
        val value = c.value as Comparable<Any>
        return when (c.op) {
            "ne" -> cb.notEqual(path, value)
            "gt" -> cb.greaterThan(path, value)
            "gte" -> cb.greaterThanOrEqualTo(path, value)
            "lt" -> cb.lessThan(path, value)
            "lte" -> cb.lessThanOrEqualTo(path, value)
            else -> cb.equal(path, value)
        }
    }
}
