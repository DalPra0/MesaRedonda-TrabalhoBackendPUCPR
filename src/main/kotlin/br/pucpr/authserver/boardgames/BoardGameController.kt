package br.pucpr.authserver.boardgames

import br.pucpr.authserver.boardgames.requests.CreateBoardGameRequest
import br.pucpr.authserver.boardgames.requests.UpdateBoardGameRequest
import br.pucpr.authserver.boardgames.responses.BoardGameResponse
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
@RequestMapping("/board-games")
class BoardGameController(val service: BoardGameService) {
    @Operation(summary = "Lista jogos com filtro dinâmico", description = "Campos: name, description, minPlayers, maxPlayers, durationMinutes, weight. Use campo=valor ou campo.operador=valor (operadores: eq, ne, gt, gte, lt, lte, like). Ex.: ?durationMinutes.lte=60&weight.gte=2&name.like=cat&tag=coop&sort=name,asc")
    @GetMapping
    fun list(
        @RequestParam tag: String? = null,
        @RequestParam minPlayers: Int? = null,
        @RequestParam maxPlayers: Int? = null,
        @RequestParam name: String? = null,
        @RequestParam maxDuration: Int? = null,
        @RequestParam weight: Int? = null,
        @RequestParam sort: String? = null,
        @Parameter(hidden = true) @RequestParam filters: Map<String, String> = emptyMap(),
    ) = service.search(tag, minPlayers, maxPlayers, name, sort, maxDuration, weight, filters)
        .map { BoardGameResponse(it) }
        .let { ResponseEntity.ok(it) }

    @GetMapping("/{gameId}")
    fun getById(@PathVariable gameId: Long) = service.findById(gameId)
        .let { BoardGameResponse(it) }
        .let { ResponseEntity.ok(it) }

    @PostMapping
    fun insert(@Valid @RequestBody game: CreateBoardGameRequest) =
        service.insert(game.toBoardGame())
            .let { BoardGameResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PutMapping("/{gameId}")
    fun update(
        @PathVariable gameId: Long,
        @Valid @RequestBody game: UpdateBoardGameRequest,
    ) = service.update(gameId, game.toBoardGame())
        .let { BoardGameResponse(it) }
        .let { ResponseEntity.ok(it) }

    @SecurityRequirement(name = "jwt-auth")
    @DeleteMapping("/{gameId}")
    fun delete(@PathVariable gameId: Long): ResponseEntity<Void> {
        service.delete(gameId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{gameId}/tags/{tagId}")
    fun addTag(
        @PathVariable gameId: Long,
        @PathVariable tagId: Long,
    ): ResponseEntity<Void> = service.addTag(gameId, tagId)
        .let {
            if (it) ResponseEntity.ok().build()
            else ResponseEntity.noContent().build()
        }

    @SecurityRequirement(name = "jwt-auth")
    @DeleteMapping("/{gameId}/tags/{tagId}")
    fun removeTag(
        @PathVariable gameId: Long,
        @PathVariable tagId: Long,
    ): ResponseEntity<Void> = service.removeTag(gameId, tagId)
        .let {
            if (it) ResponseEntity.ok().build()
            else ResponseEntity.noContent().build()
        }
}
