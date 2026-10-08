package br.pucpr.authserver.users

import br.pucpr.authserver.exceptions.BadRequestException
import br.pucpr.authserver.roles.RoleRepository
import br.pucpr.authserver.security.Jwt
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserServiceTest {

    private val repository = mockk<UserRepository>()
    private val roleRepository = mockk<RoleRepository>()
    private val jwt = mockk<Jwt>()

    private val service = UserService(repository, roleRepository, jwt)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        checkUnnecessaryStub(repository, roleRepository, jwt)
    }

    @Test
    fun `insert should save and return user when email does not exist`() {
        val user = oneUser()
        every { repository.findByEmail(user.email) } returns null
        every { repository.save(user) } returns user

        val result = service.insert(user)

        result shouldBe user
        verify(exactly = 1) { repository.findByEmail(user.email) }
    }

    @Test
    fun `insert should throw BadRequestException when user already exists`() {
        val user = oneUser()
        val existingUser = oneUser(id = 2L)
        every { repository.findByEmail(user.email) } returns existingUser

        val exception = shouldThrow<BadRequestException> {
            service.insert(user)
        }

        exception.message shouldBe "User already exists"
    }
}