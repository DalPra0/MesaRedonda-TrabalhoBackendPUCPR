package br.pucpr.authserver.music

import br.pucpr.authserver.music.requests.CastVoteRequest
import br.pucpr.authserver.music.requests.EnqueueRequest
import br.pucpr.authserver.music.responses.QueueEntryResponse
import br.pucpr.authserver.music.responses.VoteResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/game-nights/{nightId}/queue")
class QueueController(val service: MusicService) {
    @GetMapping
    fun list(
        @PathVariable nightId: Long,
        @RequestParam status: QueueStatus? = null,
        @RequestParam sort: String? = null,
    ): ResponseEntity<List<QueueEntryResponse>> {
        val entries = service.list(nightId, status, sort)
        val votes = service.votesOf(entries.mapNotNull { it.id })
        return ResponseEntity.ok(entries.map { QueueEntryResponse(it, votes[it.id].orEmpty()) })
    }

    @PostMapping
    fun enqueue(
        @PathVariable nightId: Long,
        @Valid @RequestBody request: EnqueueRequest,
    ) = service.enqueue(nightId, request)
        .let { respond(it) }
        .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    /**
     * Não apaga a linha. O status passa a REMOVED e a posição permanece,
     * para a próxima música continuar entrando no fim da fila.
     */
    @SecurityRequirement(name = "jwt-auth")
    @DeleteMapping("/{entryId}")
    fun remove(
        @PathVariable nightId: Long,
        @PathVariable entryId: Long,
    ): ResponseEntity<Void> {
        service.remove(nightId, entryId)
        return ResponseEntity.noContent().build()
    }

    @SecurityRequirement(name = "jwt-auth")
    @PostMapping("/{entryId}/play")
    fun play(
        @PathVariable nightId: Long,
        @PathVariable entryId: Long,
    ) = service.play(nightId, entryId)
        .let { respond(it) }
        .let { ResponseEntity.ok(it) }

    @SecurityRequirement(name = "jwt-auth")
    @PostMapping("/{entryId}/skip")
    fun skip(
        @PathVariable nightId: Long,
        @PathVariable entryId: Long,
    ) = service.skip(nightId, entryId)
        .let { respond(it) }
        .let { ResponseEntity.ok(it) }

    /**
     * Um voto por membro. Se ele já votou nesta entrada, o tipo é atualizado (HTTP 200).
     * O primeiro voto responde HTTP 201.
     */
    @PostMapping("/{entryId}/votes")
    fun vote(
        @PathVariable nightId: Long,
        @PathVariable entryId: Long,
        @Valid @RequestBody request: CastVoteRequest,
    ): ResponseEntity<VoteResponse> {
        val cast = service.vote(nightId, entryId, request)
        val body = VoteResponse(cast.vote)
        return if (cast.created) {
            ResponseEntity.status(HttpStatus.CREATED).body(body)
        } else {
            ResponseEntity.ok(body)
        }
    }

    private fun respond(entry: QueueEntry): QueueEntryResponse {
        val votes = service.votesOf(listOfNotNull(entry.id))
        return QueueEntryResponse(entry, votes[entry.id].orEmpty())
    }
}
