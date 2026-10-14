package br.pucpr.authserver.nights

import br.pucpr.authserver.boardgames.BoardGame
import br.pucpr.authserver.boardgames.BoardGameRepository
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
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
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@AutoConfigureMockMvc
class GameNightWebTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var gameNightRepository: GameNightRepository

    @Autowired
    lateinit var nightGameRepository: NightGameRepository

    @Autowired
    lateinit var registrationRepository: NightRegistrationRepository

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var boardGameRepository: BoardGameRepository

    @Autowired
<<<<<<< HEAD
    lateinit var scoreEntryRepository: ScoreEntryRepository

    @Autowired
=======
>>>>>>> origin/main
    lateinit var transactionManager: PlatformTransactionManager

    private val json = JsonMapper.builder().build()

    @BeforeEach
    fun cleanNights() {
        TransactionTemplate(transactionManager).executeWithoutResult {
<<<<<<< HEAD
            scoreEntryRepository.deleteAll()
            scoreEntryRepository.flush()
=======
>>>>>>> origin/main
            nightGameRepository.deleteAll()
            nightGameRepository.flush()
            registrationRepository.deleteAll()
            registrationRepository.flush()
            gameNightRepository.deleteAll()
            gameNightRepository.flush()
            boardGameRepository.findAll().forEach { game -> game.tags.clear() }
            boardGameRepository.flush()
            boardGameRepository.deleteAll()
<<<<<<< HEAD
            memberRepository.findAll().forEach { it.seasonPoints = 0 }
            memberRepository.flush()
=======
>>>>>>> origin/main
        }
    }

    @Test
    fun `catalog of members is seeded and nights support crud filter sort and jwt delete`() {
        memberRepository.findAll().map { it.displayName }
            .shouldContainAll("Lucas", "Ana", "Bruno", "Carla")
        memberRepository.findByDisplayName("Lucas")!!.seasonPoints shouldBe 0

        val early = mockMvc.perform(
            post("/game-nights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(nightJson("Abertura", "2026-10-02T19:00:00Z", "Sala 1"))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.title").value("Abertura"))
            .andReturn()
        val earlyId = json.readTree(early.response.contentAsString).get("id").asLong()

        val late = mockMvc.perform(
            post("/game-nights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(nightJson("Final", "2026-10-20T19:00:00Z", "Casa do Lucas", "OPEN"))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andReturn()
        val lateId = json.readTree(late.response.contentAsString).get("id").asLong()

        mockMvc.perform(get("/game-nights"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(2)))
            .andExpect(jsonPath("$[0].title").value("Final"))
            .andExpect(jsonPath("$[1].title").value("Abertura"))

        mockMvc.perform(get("/game-nights").param("status", "OPEN"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(1)))
            .andExpect(jsonPath("$[0].title").value("Final"))

        mockMvc.perform(
            get("/game-nights")
                .param("from", "2026-10-10T00:00:00Z")
                .param("to", "2026-10-31T00:00:00Z")
                .param("sort", "title,asc")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(1)))
            .andExpect(jsonPath("$[0].id").value(lateId))

        mockMvc.perform(get("/game-nights").param("sort", "price,asc"))
            .andExpect(status().isBadRequest)

        mockMvc.perform(
            get("/game-nights")
                .param("from", "2026-11-01T00:00:00Z")
                .param("to", "2026-10-01T00:00:00Z")
        ).andExpect(status().isBadRequest)

        mockMvc.perform(
            put("/game-nights/$earlyId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(nightJson("Abertura", "2026-10-03T19:00:00Z", "Lab 2", "DRAFT"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.location").value("Lab 2"))

        mockMvc.perform(get("/game-nights/$earlyId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.games", hasSize<Collection<Any>>(0)))
            .andExpect(jsonPath("$.members", hasSize<Collection<Any>>(0)))

        mockMvc.perform(
            post("/game-nights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(nightJson(" ", "2026-10-03T19:00:00Z", "Lab 2"))
        ).andExpect(status().isBadRequest)

        mockMvc.perform(get("/game-nights/999999"))
            .andExpect(status().isNotFound)

        mockMvc.perform(delete("/game-nights/$earlyId"))
            .andExpect(status().isForbidden)

        val token = loginToken()

        mockMvc.perform(
            delete("/game-nights/$earlyId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        mockMvc.perform(get("/game-nights/$earlyId"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/board-games"))
            .andExpect(status().isOk)
        mockMvc.perform(get("/tags"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].slug", containsInAnyOrder("party", "euro", "coop", "deducao")))
        mockMvc.perform(
            post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"admin@authserver.com","password":"admin"}""")
        ).andExpect(status().isOk)
    }

    @Test
    fun `duplicate game or member association returns 204 and draft confirm follows the status rule`() {
        val catanId = saveGame("Catan")
        val azulId = saveGame("Azul")
        val lucasId = memberRepository.findByDisplayName("Lucas")!!.id!!
        val anaId = memberRepository.findByDisplayName("Ana")!!.id!!

        val created = mockMvc.perform(
            post("/game-nights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(nightJson("Rodada 3", "2026-10-16T22:00:00Z", "Sala 1"))
        )
            .andExpect(status().isCreated)
            .andReturn()
        val nightId = json.readTree(created.response.contentAsString).get("id").asLong()

        mockMvc.perform(post("/game-nights/$nightId/games/$catanId"))
            .andExpect(status().isOk)
        mockMvc.perform(post("/game-nights/$nightId/games/$azulId"))
            .andExpect(status().isOk)
        mockMvc.perform(post("/game-nights/$nightId/games/$catanId"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/game-nights/$nightId/games"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(2)))
            .andExpect(jsonPath("$[0].name").value("Catan"))
            .andExpect(jsonPath("$[0].draftOrder").value(1))
            .andExpect(jsonPath("$[0].confirmed").value(false))
            .andExpect(jsonPath("$[1].name").value("Azul"))
            .andExpect(jsonPath("$[1].draftOrder").value(2))

        mockMvc.perform(post("/game-nights/$nightId/games/999999"))
            .andExpect(status().isNotFound)

        mockMvc.perform(post("/game-nights/$nightId/members/$lucasId"))
            .andExpect(status().isOk)
        mockMvc.perform(
            post("/game-nights/$nightId/members/$anaId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"role":"HOST"}""")
        ).andExpect(status().isOk)
        mockMvc.perform(
            post("/game-nights/$nightId/members/$lucasId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"role":"DJ"}""")
        ).andExpect(status().isNoContent)

        mockMvc.perform(get("/game-nights/$nightId/members"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(2)))
            .andExpect(jsonPath("$[0].displayName").value("Ana"))
            .andExpect(jsonPath("$[0].role").value("HOST"))
            .andExpect(jsonPath("$[0].seasonPoints").value(0))
            .andExpect(jsonPath("$[1].displayName").value("Lucas"))
            .andExpect(jsonPath("$[1].role").value("PLAYER"))

        mockMvc.perform(post("/game-nights/$nightId/members/999999"))
            .andExpect(status().isNotFound)

        mockMvc.perform(delete("/game-nights/$nightId/games/$azulId"))
            .andExpect(status().isForbidden)
        mockMvc.perform(delete("/game-nights/$nightId/members/$anaId"))
            .andExpect(status().isForbidden)
        mockMvc.perform(post("/game-nights/$nightId/draft/confirm"))
            .andExpect(status().isForbidden)

        val token = loginToken()

        mockMvc.perform(
            post("/game-nights/$nightId/draft/confirm")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("OPEN"))

        mockMvc.perform(get("/game-nights/$nightId/games"))
            .andExpect(jsonPath("$[0].confirmed").value(true))
            .andExpect(jsonPath("$[1].confirmed").value(true))

        mockMvc.perform(
            post("/game-nights/$nightId/draft/confirm")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("LIVE"))

        mockMvc.perform(
            post("/game-nights/$nightId/draft/confirm")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("LIVE"))

        mockMvc.perform(
            delete("/game-nights/$nightId/games/$azulId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/game-nights/$nightId/games"))
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(1)))
            .andExpect(jsonPath("$[0].name").value("Catan"))

        mockMvc.perform(
            delete("/game-nights/$nightId/members/$anaId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/game-nights/$nightId/members"))
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(1)))
            .andExpect(jsonPath("$[0].displayName").value("Lucas"))

        mockMvc.perform(
            put("/game-nights/$nightId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(nightJson("Rodada 3", "2026-10-16T22:00:00Z", "Sala 1", "CANCELLED"))
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/game-nights/$nightId/draft/confirm")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isBadRequest)
    }

    private fun saveGame(name: String): Long {
        val saved = boardGameRepository.save(
            BoardGame(
                name = name,
                minPlayers = 2,
                maxPlayers = 4,
                durationMinutes = 60,
                weight = 2,
                description = name,
            )
        )
        return saved.id!!
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

    private fun nightJson(
        title: String,
        scheduledAt: String,
        location: String,
        status: String? = null,
    ): String {
        val node = json.createObjectNode().apply {
            put("title", title)
            put("scheduledAt", scheduledAt)
            put("location", location)
            if (status != null) put("status", status)
        }
        return json.writeValueAsString(node)
    }
}
