package br.pucpr.authserver.nights

import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.exceptions.BadRequestException
import br.pucpr.authserver.filters.DynamicFilter
import br.pucpr.authserver.users.SortDir
import br.pucpr.authserver.music.VoteRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class MemberService(
    val repository: MemberRepository,
    val registrationRepository: NightRegistrationRepository,
    val scoreRepository: ScoreEntryRepository,
    val voteRepository: VoteRepository,
) {
    fun search(name: String?, sort: String? = null, filters: Map<String, String> = emptyMap()): List<Member> {
        val dynamic = DynamicFilter.of<Member>(filters, FILTER_FIELDS, NAMED_PARAMS)
        val byName = name?.trim()?.ifBlank { null }?.let { DynamicFilter.of<Member>(mapOf("displayName.like" to it), FILTER_FIELDS) }
        val spec = byName?.and(dynamic) ?: dynamic
        return repository.findAll(spec, parseSort(sort))
    }

    private fun parseSort(sort: String?): Sort {
        val raw = sort?.trim()?.ifBlank { null } ?: return Sort.by(Sort.Direction.ASC, "displayName")
        val parts = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty() || parts.size > 2) throw BadRequestException("Invalid sort")
        if (parts[0] !in SORT_FIELDS) throw BadRequestException("Invalid sort field")
        val direction = if (parts.size == 2) SortDir.find(parts[1]) else SortDir.ASC
        return Sort.by(if (direction == SortDir.DESC) Sort.Direction.DESC else Sort.Direction.ASC, parts[0])
    }

    fun findById(id: Long) = repository.findByIdOrNull(id) ?: throw NotFoundException(id)

    fun insert(member: Member): Member {
        validate(member)
        if (repository.findByDisplayName(member.displayName) != null) {
            throw BadRequestException("Member already exists")
        }
        val saved = repository.save(member)
        log.info("Member ${saved.id} created")
        return saved
    }

    fun update(id: Long, updated: Member): Member {
        validate(updated)
        val member = findById(id)
        val sameName = repository.findByDisplayName(updated.displayName)
        if (sameName != null && sameName.id != id) {
            throw BadRequestException("Member already exists")
        }
        member.displayName = updated.displayName
        member.email = updated.email
        val saved = repository.save(member)
        log.info("Member ${saved.id} updated")
        return saved
    }

    // Membro com histórico (noites, placares, votos) não some: apagar levaria o placar junto.
    fun delete(id: Long) {
        val member = findById(id)
        if (registrationRepository.existsByMemberId(id) ||
            scoreRepository.existsByMemberId(id) ||
            voteRepository.existsByMemberId(id)
        ) {
            throw BadRequestException("Member has game nights, scores or votes and cannot be deleted")
        }
        repository.delete(member)
        log.warn("Member $id deleted")
    }

    private fun validate(member: Member) {
        if (member.displayName.isBlank()) {
            throw BadRequestException("displayName is required")
        }
    }

    companion object {
        private val SORT_FIELDS = setOf("displayName", "email", "seasonPoints")
        private val NAMED_PARAMS = setOf("name", "sort")
        private val FILTER_FIELDS = mapOf(
            "displayName" to String::class.java,
            "email" to String::class.java,
            "seasonPoints" to Int::class.javaObjectType,
        )
        val log = LoggerFactory.getLogger(MemberService::class.java)
    }
}
