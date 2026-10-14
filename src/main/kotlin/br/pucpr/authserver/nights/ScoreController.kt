package br.pucpr.authserver.nights

import br.pucpr.authserver.nights.requests.CreateScoreRequest
import br.pucpr.authserver.nights.requests.UpdateScoreRequest
import br.pucpr.authserver.nights.responses.ScoreResponse
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/game-nights/{nightId}/scores")
class ScoreController(val service: ScoreService) {
    @GetMapping
    fun list(@PathVariable nightId: Long) = service.list(nightId)
        .map { ScoreResponse(it) }
        .let { ResponseEntity.ok(it) }

    @PostMapping
    fun insert(
        @PathVariable nightId: Long,
        @Valid @RequestBody score: CreateScoreRequest,
    ) = service.insert(nightId, score)
        .let { ScoreResponse(it) }
        .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PutMapping("/{scoreId}")
    fun update(
        @PathVariable nightId: Long,
        @PathVariable scoreId: Long,
        @Valid @RequestBody score: UpdateScoreRequest,
    ) = service.update(nightId, scoreId, score)
        .let { ScoreResponse(it) }
        .let { ResponseEntity.ok(it) }

    @SecurityRequirement(name = "jwt-auth")
    @DeleteMapping("/{scoreId}")
    fun delete(
        @PathVariable nightId: Long,
        @PathVariable scoreId: Long,
    ): ResponseEntity<Void> {
        service.delete(nightId, scoreId)
        return ResponseEntity.noContent().build()
    }
}
