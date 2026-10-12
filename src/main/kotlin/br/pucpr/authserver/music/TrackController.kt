package br.pucpr.authserver.music

import br.pucpr.authserver.music.requests.CreateTrackRequest
import br.pucpr.authserver.music.responses.TrackResponse
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
@RequestMapping("/tracks")
class TrackController(val service: TrackService) {
    @GetMapping
    fun list(
        @RequestParam artist: String? = null,
        @RequestParam title: String? = null,
        @RequestParam sort: String? = null,
    ) = service.search(artist, title, sort)
        .map { TrackResponse(it) }
        .let { ResponseEntity.ok(it) }

    @GetMapping("/{trackId}")
    fun getById(@PathVariable trackId: Long) = service.findById(trackId)
        .let { TrackResponse(it) }
        .let { ResponseEntity.ok(it) }

    @PostMapping
    fun insert(@Valid @RequestBody track: CreateTrackRequest) =
        service.insert(track.toTrack())
            .let { TrackResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @SecurityRequirement(name = "jwt-auth")
    @DeleteMapping("/{trackId}")
    fun delete(@PathVariable trackId: Long): ResponseEntity<Void> {
        service.delete(trackId)
        return ResponseEntity.noContent().build()
    }
}
