package br.pucpr.authserver.users

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FixturesTest {

    @Test
    fun `oneUser should generate default email based on default name`() {
        val user = oneUser()
        user.name shouldBe "User"
        user.email shouldBe "user@email.com"
        user.id shouldBe 1L
        user.password shouldBe "password"
        user.roles shouldBe mutableSetOf()
    }

    @Test
    fun `oneUser should generate email with lowercase and dots for spaces based on name`() {
        val user = oneUser(name = "Vinicius Godoy")
        user.name shouldBe "Vinicius Godoy"
        user.email shouldBe "vinicius.godoy@email.com"
    }

    @Test
    fun `oneUser should allow custom email overriding default`() {
        val user = oneUser(name = "Vinicius Godoy", email = "custom@example.com")
        user.name shouldBe "Vinicius Godoy"
        user.email shouldBe "custom@example.com"
    }

    @Test
    fun `manyUsers with varargs should create users with proper ids and names`() {
        val users = manyUsers("Vinicius Godoy", "John Doe")

        users shouldHaveSize 2
        users[0].id shouldBe 1L
        users[0].name shouldBe "Vinicius Godoy"
        users[0].email shouldBe "vinicius.godoy@email.com"

        users[1].id shouldBe 2L
        users[1].name shouldBe "John Doe"
        users[1].email shouldBe "john.doe@email.com"
    }

    @Test
    fun `manyUsers with list should create users with proper ids and names`() {
        val users = manyUsers(listOf("Alice Smith", "Bob"))

        users shouldHaveSize 2
        users[0].id shouldBe 1L
        users[0].name shouldBe "Alice Smith"
        users[0].email shouldBe "alice.smith@email.com"

        users[1].id shouldBe 2L
        users[1].name shouldBe "Bob"
        users[1].email shouldBe "bob@email.com"
    }
}
