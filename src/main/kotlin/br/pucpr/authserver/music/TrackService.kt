package br.pucpr.authserver.music

import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.exceptions.BadRequestException
import br.pucpr.authserver.filters.DynamicFilter
import br.pucpr.authserver.users.SortDir
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TrackService(
    val repository: TrackRepository,
    val queueEntryRepository: QueueEntryRepository,
) {
    fun insert(track: Track): Track {
        validate(track)
        val saved = repository.save(track)
        log.info("Track ${saved.id} created")
        return saved
    }

    fun search(
        artist: String?,
        title: String?,
        sort: String?,
        filters: Map<String, String> = emptyMap(),
    ): List<Track> {
        val dynamic = DynamicFilter.of<Track>(filters, FILTER_FIELDS, NAMED_PARAMS)
        return repository.findAll(
            trackSearch(artist = artist.blankToNull(), title = title.blankToNull()).and(dynamic),
            parseSort(sort),
        )
    }

    fun findById(id: Long) = repository.findByIdOrNull(id) ?: throw NotFoundException(id)

    /**
     * Apaga a faixa. Se ela ainda está em alguma fila, recusa com [BadRequestException]
     * para não derrubar a entrada da noite.
     */
    @Transactional
    fun delete(id: Long) {
        val track = findById(id)
        if (queueEntryRepository.countByTrackId(id) > 0) {
            throw BadRequestException("Track is in a queue")
        }
        repository.delete(track)
        log.warn("Track $id deleted")
    }

    private fun validate(track: Track) {
        if (track.title.isBlank()) throw BadRequestException("title is required")
        if (track.artist.isBlank()) throw BadRequestException("artist is required")
        if (track.durationSeconds < 1) throw BadRequestException("durationSeconds must be at least 1")
    }

    private fun parseSort(sort: String?): Sort {
        val raw = sort.blankToNull() ?: return Sort.by(Sort.Direction.ASC, "title")
        val parts = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty() || parts.size > 2) {
            throw BadRequestException("Invalid sort")
        }
        val field = parts[0]
        if (field !in SORT_FIELDS) {
            throw BadRequestException("Invalid sort field")
        }
        val direction = if (parts.size == 2) SortDir.find(parts[1]) else SortDir.ASC
        val springDirection = when (direction) {
            SortDir.ASC -> Sort.Direction.ASC
            SortDir.DESC -> Sort.Direction.DESC
        }
        return Sort.by(springDirection, field)
    }

    private fun String?.blankToNull() = this?.trim()?.ifBlank { null }

    companion object {
        private val NAMED_PARAMS = setOf("artist", "title", "sort")
        private val FILTER_FIELDS = mapOf(
            "title" to String::class.java,
            "artist" to String::class.java,
            "durationSeconds" to Int::class.javaObjectType,
        )
        private val SORT_FIELDS = setOf("title", "artist", "durationSeconds")
        private val log = LoggerFactory.getLogger(TrackService::class.java)
    }
}
