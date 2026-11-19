package br.pucpr.authserver.exceptions

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class ApiExceptionHandlerWebTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `validation errors come as problem detail with the errors field`() {
        mockMvc.perform(
            post("/board-games").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"","minPlayers":0,"maxPlayers":4,"durationMinutes":30,"weight":9}""")
        )
            .andExpect(status().`is`(422))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.detail").value("Invalid request body"))
            .andExpect(jsonPath("$.errors.name").isArray)
            .andExpect(jsonPath("$.errors.minPlayers").isArray)
            .andExpect(jsonPath("$.errors.weight").isArray)
    }

    @Test
    fun `malformed json is a 400 with the body error`() {
        mockMvc.perform(
            post("/board-games").contentType(MediaType.APPLICATION_JSON).content("{ isso nao e json")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Invalid JSON request"))
            .andExpect(jsonPath("$.errors.body[0]").value("Malformed JSON request"))
    }

    @Test
    fun `domain exceptions keep the status of their ResponseStatus and the message`() {
        mockMvc.perform(get("/board-games/999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Not found. id=999999"))
            .andExpect(jsonPath("$.detail").value("Not found. id=999999"))

        mockMvc.perform(get("/board-games").param("sort", "preco"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Invalid sort field"))
    }

    @Test
    fun `health is public and protected routes answer 401 without token`() {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("OK"))

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/board-games/1"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/board-games/1")
                .header("Authorization", "Bearer token-invalido")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `invalid enum value names the field and wrong url type is a 400`() {
        mockMvc.perform(
            post("/game-nights/1/members/1").contentType(MediaType.APPLICATION_JSON)
                .content("""{"role":"CHEFE_SUPREMO"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Invalid JSON request"))
            .andExpect(jsonPath("$.errors.role[0]").value("Invalid type. Expected NightRole"))

        mockMvc.perform(get("/board-games/abc"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Invalid URL parameter"))
            .andExpect(jsonPath("$.errors.gameId[0]").value("Invalid type. Expected long"))
    }
}
