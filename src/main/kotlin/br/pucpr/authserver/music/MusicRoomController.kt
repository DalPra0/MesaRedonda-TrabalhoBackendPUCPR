package br.pucpr.authserver.music

import br.pucpr.authserver.music.requests.CreateMusicRoomRequest
import br.pucpr.authserver.music.responses.MusicRoomResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/game-nights/{nightId}/music-room")
class MusicRoomController(val service: MusicService) {
    @GetMapping
    fun get(@PathVariable nightId: Long) = service.findRoom(nightId)
        .let { MusicRoomResponse(it) }
        .let { ResponseEntity.ok(it) }

    /**
     * Cria a sala 1:1 da noite. Se ela já existe, HTTP 200 com a sala atual,
     * sem alterar inviteCode, mood ou active. Sala nova responde HTTP 201.
     */
    @SecurityRequirement(name = "jwt-auth")
    @PostMapping
    fun open(
        @PathVariable nightId: Long,
        @RequestBody(required = false) body: CreateMusicRoomRequest?,
    ): ResponseEntity<MusicRoomResponse> {
        val opened = service.open(nightId, body)
        val bodyResponse = MusicRoomResponse(opened.room)
        return if (opened.created) {
            ResponseEntity.status(HttpStatus.CREATED).body(bodyResponse)
        } else {
            ResponseEntity.ok(bodyResponse)
        }
    }

    @SecurityRequirement(name = "jwt-auth")
    @DeleteMapping
    fun delete(@PathVariable nightId: Long): ResponseEntity<Void> {
        service.delete(nightId)
        return ResponseEntity.noContent().build()
    }
}
