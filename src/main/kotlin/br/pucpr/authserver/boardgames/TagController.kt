package br.pucpr.authserver.boardgames

import br.pucpr.authserver.boardgames.requests.TagRequest
import br.pucpr.authserver.boardgames.responses.TagResponse
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
@RequestMapping("/tags")
class TagController(val service: TagService) {
    @GetMapping
    fun list() = service.findAll()
        .map { TagResponse(it) }
        .let { ResponseEntity.ok(it) }

    @GetMapping("/{tagId}")
    fun getById(@PathVariable tagId: Long) = service.findById(tagId)
        .let { TagResponse(it) }
        .let { ResponseEntity.ok(it) }

    @PostMapping
    fun insert(@Valid @RequestBody tag: TagRequest) =
        service.insert(tag.toTag())
            .let { TagResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PutMapping("/{tagId}")
    fun update(
        @PathVariable tagId: Long,
        @Valid @RequestBody tag: TagRequest,
    ) = service.update(tagId, tag.toTag())
        .let { TagResponse(it) }
        .let { ResponseEntity.ok(it) }

    @SecurityRequirement(name = "jwt-auth")
    @DeleteMapping("/{tagId}")
    fun delete(@PathVariable tagId: Long): ResponseEntity<Void> {
        service.delete(tagId)
        return ResponseEntity.noContent().build()
    }
}
