package br.pucpr.authserver.nights

import br.pucpr.authserver.nights.requests.CreateGameNightRequest
import br.pucpr.authserver.nights.requests.RegisterMemberRequest
import br.pucpr.authserver.nights.requests.UpdateGameNightRequest
import br.pucpr.authserver.nights.responses.GameNightDetailResponse
import br.pucpr.authserver.nights.responses.GameNightResponse
import br.pucpr.authserver.nights.responses.NightGameResponse
import br.pucpr.authserver.nights.responses.NightMemberResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
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
import java.time.Instant

@RestController
@RequestMapping("/game-nights")
class GameNightController(val service: GameNightService) {
    @GetMapping
    fun list(
        @RequestParam status: GameNightStatus? = null,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant? = null,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant? = null,
        @RequestParam sort: String? = null,
    ) = service.search(status, from, to, sort)
        .map { GameNightResponse(it) }
        .let { ResponseEntity.ok(it) }

    @GetMapping("/{nightId}")
    fun getById(@PathVariable nightId: Long) = GameNightDetailResponse(
        night = service.findById(nightId),
        games = service.listGames(nightId),
        members = service.listMembers(nightId),
    ).let { ResponseEntity.ok(it) }

    @PostMapping
    fun insert(@Valid @RequestBody night: CreateGameNightRequest) =
        service.insert(night.toGameNight())
            .let { GameNightResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PutMapping("/{nightId}")
    fun update(
        @PathVariable nightId: Long,
        @Valid @RequestBody night: UpdateGameNightRequest,
    ) = service.update(nightId, night.toGameNight())
        .let { GameNightResponse(it) }
        .let { ResponseEntity.ok(it) }

    @SecurityRequirement(name = "jwt-auth")
    @DeleteMapping("/{nightId}")
    fun delete(@PathVariable nightId: Long): ResponseEntity<Void> {
        service.delete(nightId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{nightId}/games")
    fun listGames(@PathVariable nightId: Long) = service.listGames(nightId)
        .map { NightGameResponse(it) }
        .let { ResponseEntity.ok(it) }

    @PostMapping("/{nightId}/games/{gameId}")
    fun addGame(
        @PathVariable nightId: Long,
        @PathVariable gameId: Long,
    ): ResponseEntity<Void> = service.addGame(nightId, gameId)
        .let { created ->
            if (created) ResponseEntity.ok().build()
            else ResponseEntity.noContent().build()
        }

    @SecurityRequirement(name = "jwt-auth")
    @DeleteMapping("/{nightId}/games/{gameId}")
    fun removeGame(
        @PathVariable nightId: Long,
        @PathVariable gameId: Long,
    ): ResponseEntity<Void> = service.removeGame(nightId, gameId)
        .let { removed ->
            if (removed) ResponseEntity.ok().build()
            else ResponseEntity.noContent().build()
        }

    @GetMapping("/{nightId}/members")
    fun listMembers(@PathVariable nightId: Long) = service.listMembers(nightId)
        .map { NightMemberResponse(it) }
        .let { ResponseEntity.ok(it) }

    @PostMapping("/{nightId}/members/{memberId}")
    fun addMember(
        @PathVariable nightId: Long,
        @PathVariable memberId: Long,
        @RequestBody(required = false) body: RegisterMemberRequest?,
    ): ResponseEntity<Void> = service.addMember(nightId, memberId, body?.role)
        .let { created ->
            if (created) ResponseEntity.ok().build()
            else ResponseEntity.noContent().build()
        }

    @SecurityRequirement(name = "jwt-auth")
    @DeleteMapping("/{nightId}/members/{memberId}")
    fun removeMember(
        @PathVariable nightId: Long,
        @PathVariable memberId: Long,
    ): ResponseEntity<Void> = service.removeMember(nightId, memberId)
        .let { removed ->
            if (removed) ResponseEntity.ok().build()
            else ResponseEntity.noContent().build()
        }

    @SecurityRequirement(name = "jwt-auth")
    @PostMapping("/{nightId}/draft/confirm")
    fun confirmDraft(@PathVariable nightId: Long) =
        service.confirmDraft(nightId)
            .let { GameNightResponse(it) }
            .let { ResponseEntity.ok(it) }
}
