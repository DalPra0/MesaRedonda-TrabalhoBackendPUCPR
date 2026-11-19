package br.pucpr.authserver.nights

import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.exceptions.BadRequestException
import br.pucpr.authserver.music.VoteRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import java.util.Optional

class MemberServiceTest {
    private val repository = mockk<MemberRepository>()
    private val registrationRepository = mockk<NightRegistrationRepository>()
    private val scoreRepository = mockk<ScoreEntryRepository>()
    private val voteRepository = mockk<VoteRepository>()
    private val service = MemberService(repository, registrationRepository, scoreRepository, voteRepository)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        checkUnnecessaryStub(repository, registrationRepository, scoreRepository, voteRepository)
    }

    @Test
    fun `search should list everybody sorted by name when there is no filter`() {
        val members = listOf(oneMember())
        every {
            repository.findAll(any<Specification<Member>>(), Sort.by(Sort.Direction.ASC, "displayName"))
        } returns members

        service.search(null) shouldBe members
    }

    @Test
    fun `search should accept a name and a sort by season points`() {
        val members = listOf(oneMember())
        every {
            repository.findAll(any<Specification<Member>>(), Sort.by(Sort.Direction.DESC, "seasonPoints"))
        } returns members

        service.search(" luc ", "seasonPoints,desc", mapOf("seasonPoints.gte" to "10")) shouldBe members
    }

    @Test
    fun `search should reject unknown sort field and unknown filter field`() {
        shouldThrow<BadRequestException> { service.search(null, "senha,asc") }.message shouldBe "Invalid sort field"
        shouldThrow<BadRequestException> { service.search(null, null, mapOf("senha" to "1")) }.message shouldBe
            "Invalid filter field: senha"
    }

    @Test
    fun `insert should save when display name is free`() {
        val member = oneMember(id = null, displayName = "Diego")
        every { repository.findByDisplayName("Diego") } returns null
        every { repository.save(member) } returns oneMember(id = 5L, displayName = "Diego")

        service.insert(member).id shouldBe 5L
    }

    @Test
    fun `insert should throw BadRequestException when display name already exists`() {
        every { repository.findByDisplayName("Lucas") } returns oneMember()

        val exception = shouldThrow<BadRequestException> {
            service.insert(oneMember(id = null))
        }

        exception.message shouldBe "Member already exists"
    }

    @Test
    fun `insert should throw BadRequestException when display name is blank`() {
        shouldThrow<BadRequestException> {
            service.insert(oneMember(id = null, displayName = " "))
        }
    }

    @Test
    fun `update should change name and email but keep season points`() {
        val member = oneMember(seasonPoints = 21)
        every { repository.findById(1L) } returns Optional.of(member)
        every { repository.findByDisplayName("Luquinhas") } returns null
        every { repository.save(member) } returns member

        val result = service.update(1L, oneMember(id = null, displayName = "Luquinhas", email = null))

        result.displayName shouldBe "Luquinhas"
        result.email shouldBe null
        result.seasonPoints shouldBe 21
    }

    @Test
    fun `update should throw BadRequestException when name belongs to another member`() {
        every { repository.findById(1L) } returns Optional.of(oneMember())
        every { repository.findByDisplayName("Ana") } returns oneMember(id = 2L, displayName = "Ana")

        shouldThrow<BadRequestException> {
            service.update(1L, oneMember(id = null, displayName = "Ana"))
        }
    }

    @Test
    fun `update should throw NotFoundException when member does not exist`() {
        every { repository.findById(9L) } returns Optional.empty()

        shouldThrow<NotFoundException> {
            service.update(9L, oneMember(id = null))
        }
    }

    @Test
    fun `delete should remove a member without history`() {
        val member = oneMember()
        every { repository.findById(1L) } returns Optional.of(member)
        every { registrationRepository.existsByMemberId(1L) } returns false
        every { scoreRepository.existsByMemberId(1L) } returns false
        every { voteRepository.existsByMemberId(1L) } returns false
        every { repository.delete(member) } just runs

        service.delete(1L)

        verify { repository.delete(member) }
    }

    @Test
    fun `delete should throw BadRequestException when member is registered in a night`() {
        every { repository.findById(1L) } returns Optional.of(oneMember())
        every { registrationRepository.existsByMemberId(1L) } returns true

        shouldThrow<BadRequestException> {
            service.delete(1L)
        }
    }

    @Test
    fun `delete should throw BadRequestException when member has scores`() {
        every { repository.findById(1L) } returns Optional.of(oneMember())
        every { registrationRepository.existsByMemberId(1L) } returns false
        every { scoreRepository.existsByMemberId(1L) } returns true

        shouldThrow<BadRequestException> {
            service.delete(1L)
        }
    }

    @Test
    fun `delete should throw NotFoundException when member does not exist`() {
        every { repository.findById(9L) } returns Optional.empty()

        shouldThrow<NotFoundException> {
            service.delete(9L)
        }
    }
}
