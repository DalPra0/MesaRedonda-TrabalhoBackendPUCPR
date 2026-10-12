package br.pucpr.authserver.boardgames

import br.pucpr.authserver.boardgames.responses.TagResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tags")
class TagController(val service: TagService) {
    @GetMapping
    fun list() = service.findAll()
        .map { TagResponse(it) }
        .let { ResponseEntity.ok(it) }
}
