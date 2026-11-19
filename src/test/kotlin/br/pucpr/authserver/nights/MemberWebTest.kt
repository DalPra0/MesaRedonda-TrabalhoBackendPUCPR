package br.pucpr.authserver.nights

import org.hamcrest.Matchers.hasItem
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
class MemberWebTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    private val json = JsonMapper.builder().build()

    @Test
    fun `member crud with jwt on delete`() {
        val created = mockMvc.perform(
            post("/members").contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"Membro Web Test","email":"web@pucpr.edu.br"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.seasonPoints").value(0))
            .andReturn()
        val id = json.readTree(created.response.contentAsString).get("id").asLong()

        mockMvc.perform(
            post("/members").contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"Membro Web Test"}""")
        ).andExpect(status().isBadRequest)

        mockMvc.perform(
            post("/members").contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"","email":"nao-e-email"}""")
        ).andExpect(status().`is`(422))

        mockMvc.perform(get("/members").param("name", "web test"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].displayName", hasItem("Membro Web Test")))

        mockMvc.perform(
            put("/members/$id").contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"Membro Web Editado"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("Membro Web Editado"))

        mockMvc.perform(delete("/members/$id")).andExpect(status().isUnauthorized)

        val token = loginToken()
        mockMvc.perform(delete("/members/$id").header("Authorization", "Bearer $token"))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/members/$id")).andExpect(status().isNotFound)
    }

    private fun loginToken(): String {
        val login = mockMvc.perform(
            post("/users/login").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"admin@authserver.com","password":"admin"}""")
        ).andExpect(status().isOk).andReturn()
        return json.readTree(login.response.contentAsString).get("token").stringValue()
    }
}
