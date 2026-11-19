package br.pucpr.authserver.filters

import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@AutoConfigureMockMvc
class DynamicFilterWebTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    private val json = JsonMapper.builder().build()

    private fun createGame(name: String, duration: Int, weight: Int): Long {
        val r = mockMvc.perform(
            post("/board-games").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"$name","minPlayers":2,"maxPlayers":4,"durationMinutes":$duration,"weight":$weight}""")
        ).andExpect(status().isCreated).andReturn()
        return json.readTree(r.response.contentAsString).get("id").asLong()
    }

    @Test
    fun `client composes filters with field operator value on the query string`() {
        val short = createGame("DynFiltroRapido", 20, 1)
        val long = createGame("DynFiltroLongo", 120, 4)
        try {
            mockMvc.perform(get("/board-games").param("durationMinutes.lte", "60").param("name.like", "dynfiltro"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[*].name", hasItem("DynFiltroRapido")))
                .andExpect(jsonPath("$[*].name", not(hasItem("DynFiltroLongo"))))

            mockMvc.perform(
                get("/board-games").param("weight.gte", "3").param("durationMinutes.gt", "100")
                    .param("name", "dynfiltro").param("sort", "weight,desc")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[*].name", hasItem("DynFiltroLongo")))
                .andExpect(jsonPath("$[*].name", not(hasItem("DynFiltroRapido"))))

            mockMvc.perform(get("/board-games").param("preco.lte", "10"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.detail").value("Invalid filter field: preco"))

            mockMvc.perform(get("/board-games").param("weight.gte", "pesado"))
                .andExpect(status().isBadRequest)

            mockMvc.perform(get("/members").param("seasonPoints.gte", "0").param("sort", "seasonPoints,desc"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[*].displayName", hasItem("Lucas")))

            mockMvc.perform(get("/tracks").param("durationSeconds.gte", "1")).andExpect(status().isOk)
            mockMvc.perform(get("/game-nights").param("status.ne", "CANCELLED").param("title.like", "x"))
                .andExpect(status().isOk)
        } finally {
            val token = login()
            listOf(short, long).forEach {
                mockMvc.perform(delete("/board-games/$it").header("Authorization", "Bearer $token"))
            }
        }
    }

    private fun login(): String {
        val r = mockMvc.perform(
            post("/users/login").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"admin@authserver.com","password":"admin"}""")
        ).andExpect(status().isOk).andReturn()
        return json.readTree(r.response.contentAsString).get("token").stringValue()
    }
}
