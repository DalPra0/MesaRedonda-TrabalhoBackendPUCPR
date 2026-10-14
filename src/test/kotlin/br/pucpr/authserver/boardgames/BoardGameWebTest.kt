package br.pucpr.authserver.boardgames

import br.pucpr.authserver.nights.NightGameRepository
<<<<<<< HEAD
import br.pucpr.authserver.nights.ScoreEntryRepository
=======
>>>>>>> origin/main
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@AutoConfigureMockMvc
class BoardGameWebTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var boardGameRepository: BoardGameRepository

    @Autowired
    lateinit var tagRepository: TagRepository

    @Autowired
    lateinit var nightGameRepository: NightGameRepository

    @Autowired
<<<<<<< HEAD
    lateinit var scoreEntryRepository: ScoreEntryRepository

    @Autowired
=======
>>>>>>> origin/main
    lateinit var transactionManager: PlatformTransactionManager

    private val json = JsonMapper.builder().build()

    @BeforeEach
    fun cleanGames() {
        TransactionTemplate(transactionManager).executeWithoutResult {
<<<<<<< HEAD
            scoreEntryRepository.deleteAll()
            scoreEntryRepository.flush()
=======
>>>>>>> origin/main
            nightGameRepository.deleteAll()
            nightGameRepository.flush()
            boardGameRepository.findAll().forEach { game -> game.tags.clear() }
            boardGameRepository.flush()
            boardGameRepository.deleteAll()
        }
    }

    @Test
    fun `catalog crud filter and jwt delete`() {
        mockMvc.perform(get("/tags"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].slug", containsInAnyOrder("party", "euro", "coop", "deducao")))

        val partyId = tagRepository.findBySlug("party")!!.id

        val created = mockMvc.perform(
            post("/board-games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gameJson("Catan", 3, 4, 90, 2, "Colonos"))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Catan"))
            .andExpect(jsonPath("$.tags", hasSize<Collection<Any>>(0)))
            .andReturn()

        val gameId = json.readTree(created.response.contentAsString).get("id").asLong()

        mockMvc.perform(
            post("/board-games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gameJson("Azul", 2, 4, 45, 2, "Azulejos"))
        ).andExpect(status().isCreated)

        mockMvc.perform(post("/board-games/$gameId/tags/$partyId"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/board-games/$gameId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tags[0].slug").value("party"))

        mockMvc.perform(get("/board-games").param("tag", "party").param("sort", "name,asc"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(1)))
            .andExpect(jsonPath("$[0].name").value("Catan"))

        mockMvc.perform(get("/board-games").param("name", "azu").param("minPlayers", "2").param("maxPlayers", "4"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Azul"))

        mockMvc.perform(
            put("/board-games/$gameId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gameJson("Catan", 3, 5, 90, 3, "Colonos"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.maxPlayers").value(5))
            .andExpect(jsonPath("$.weight").value(3))

        mockMvc.perform(
            post("/board-games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gameJson("", 3, 4, 90, 9, "invalido"))
        ).andExpect(status().isBadRequest)

        mockMvc.perform(delete("/board-games/$gameId"))
            .andExpect(status().isForbidden)

        mockMvc.perform(delete("/board-games/$gameId/tags/$partyId"))
            .andExpect(status().isForbidden)

        val token = loginToken()

        mockMvc.perform(
            delete("/board-games/$gameId/tags/$partyId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)

        mockMvc.perform(
            delete("/board-games/$gameId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        mockMvc.perform(get("/board-games/$gameId"))
            .andExpect(status().isNotFound)

        mockMvc.perform(post("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"email":"admin@authserver.com","password":"admin"}"""))
            .andExpect(status().isOk)
    }

    private fun loginToken(): String {
        val login = mockMvc.perform(
            post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"admin@authserver.com","password":"admin"}""")
        )
            .andExpect(status().isOk)
            .andReturn()
        return json.readTree(login.response.contentAsString).get("token").stringValue()
    }

    private fun gameJson(
        name: String,
        minPlayers: Int,
        maxPlayers: Int,
        durationMinutes: Int,
        weight: Int,
        description: String,
    ): String {
        val node: JsonNode = json.createObjectNode().apply {
            put("name", name)
            put("minPlayers", minPlayers)
            put("maxPlayers", maxPlayers)
            put("durationMinutes", durationMinutes)
            put("weight", weight)
            put("description", description)
        }
        return json.writeValueAsString(node)
    }
}
