package br.pucpr.authserver.boardgames

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@AutoConfigureMockMvc
class TagWebTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    private val json = JsonMapper.builder().build()

    @Test
    fun `tag crud with jwt on delete`() {
        val created = mockMvc.perform(
            post("/tags").contentType(MediaType.APPLICATION_JSON)
                .content("""{"slug":"  Solo ","label":"Solo"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.slug").value("solo"))
            .andReturn()
        val id = json.readTree(created.response.contentAsString).get("id").asLong()

        mockMvc.perform(
            post("/tags").contentType(MediaType.APPLICATION_JSON)
                .content("""{"slug":"solo","label":"Outra"}""")
        ).andExpect(status().isBadRequest)

        mockMvc.perform(
            post("/tags").contentType(MediaType.APPLICATION_JSON)
                .content("""{"slug":"","label":""}""")
        ).andExpect(status().`is`(422))

        mockMvc.perform(get("/tags/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.label").value("Solo"))

        mockMvc.perform(
            put("/tags/$id").contentType(MediaType.APPLICATION_JSON)
                .content("""{"slug":"solo-web","label":"Jogo solo"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.slug").value("solo-web"))

        mockMvc.perform(delete("/tags/$id")).andExpect(status().isUnauthorized)

        val token = loginToken()
        mockMvc.perform(delete("/tags/$id").header("Authorization", "Bearer $token"))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/tags/$id")).andExpect(status().isNotFound)
    }

    private fun loginToken(): String {
        val login = mockMvc.perform(
            post("/users/login").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"admin@authserver.com","password":"admin"}""")
        ).andExpect(status().isOk).andReturn()
        return json.readTree(login.response.contentAsString).get("token").stringValue()
    }
}
