package br.pucpr.authserver.nights

import br.pucpr.authserver.nights.requests.MemberRequest
import br.pucpr.authserver.nights.responses.MemberResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/members")
class MemberController(val service: MemberService) {
    @Operation(summary = "Lista membros com filtro dinâmico e ranking", description = "Campos: displayName, email, seasonPoints. Operadores: eq, ne, gt, gte, lt, lte, like. Ex.: ?seasonPoints.gte=10&sort=seasonPoints,desc")
    @GetMapping
    fun list(
        @RequestParam name: String? = null,
        @RequestParam sort: String? = null,
        @Parameter(hidden = true) @RequestParam filters: Map<String, String> = emptyMap(),
    ) = service.search(name, sort, filters)
        .map { MemberResponse(it) }
        .let { ResponseEntity.ok(it) }

    @GetMapping("/{memberId}")
    fun getById(@PathVariable memberId: Long) = service.findById(memberId)
        .let { MemberResponse(it) }
        .let { ResponseEntity.ok(it) }

    @PostMapping
    fun insert(@Valid @RequestBody member: MemberRequest) =
        service.insert(member.toMember())
            .let { MemberResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PutMapping("/{memberId}")
    fun update(
        @PathVariable memberId: Long,
        @Valid @RequestBody member: MemberRequest,
    ) = service.update(memberId, member.toMember())
        .let { MemberResponse(it) }
        .let { ResponseEntity.ok(it) }

    @SecurityRequirement(name = "jwt-auth")
    @DeleteMapping("/{memberId}")
    fun delete(@PathVariable memberId: Long): ResponseEntity<Void> {
        service.delete(memberId)
        return ResponseEntity.noContent().build()
    }
}
