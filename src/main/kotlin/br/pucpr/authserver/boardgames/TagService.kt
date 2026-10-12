package br.pucpr.authserver.boardgames

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class TagService(val repository: TagRepository) {
    fun findAll() = repository.findAll(Sort.by("slug").ascending())
}
