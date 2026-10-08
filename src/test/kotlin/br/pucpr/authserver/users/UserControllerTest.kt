package br.pucpr.authserver.users

import br.pucpr.authserver.exceptions.BadRequestException
import br.pucpr.authserver.users.requests.CreateUserRequest
import br.pucpr.authserver.users.responses.UserResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class UserControllerTest {

    private val service = mockk<UserService>()
    private val controller = UserController(service)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        checkUnnecessaryStub(service)
    }

    @Test
    fun `insert should create and return user response with status 201`() {
        val request = CreateUserRequest(
            email = "user@email.com",
            password = "Password123!",
            name = "User"
        )
        val createdUser = oneUser(
            id = 1L,
            email = "user@email.com",
            password = "Password123!",
            name = "User"
        )

        every {
            service.insert(match {
                it.email == request.email &&
                        it.password == request.password &&
                        it.name == request.name
            })
        } returns createdUser

        val response = controller.insert(request)

        response.statusCode shouldBe HttpStatus.CREATED
        response.body shouldBe UserResponse(createdUser)
    }

    @Test
    fun `insert should propagate exception when service throws BadRequestException`() {
        val request = CreateUserRequest(
            email = "user@email.com",
            password = "Password123!",
            name = "User"
        )

        every {
            service.insert(match {
                it.email == request.email &&
                        it.password == request.password &&
                        it.name == request.name
            })
        } throws BadRequestException("User already exists")

        val exception = shouldThrow<BadRequestException> {
            controller.insert(request)
        }

        exception.message shouldBe "User already exists"
    }
}