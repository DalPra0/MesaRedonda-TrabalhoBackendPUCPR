package br.pucpr.authserver.nights

import br.pucpr.authserver.boardgames.BoardGame
import br.pucpr.authserver.boardgames.BoardGameRepository
import br.pucpr.authserver.music.MusicRoomRepository
import br.pucpr.authserver.music.QueueEntryRepository
import br.pucpr.authserver.music.TrackRepository
import br.pucpr.authserver.music.VoteRepository
import io.kotest.matchers.shouldBe
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@AutoConfigureMockMvc
class ScoreWebTest {
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
    lateinit var scoreEntryRepository: ScoreEntryRepository

    @Autowired
    lateinit var voteRepository: VoteRepository

    @Autowired
    lateinit var queueEntryRepository: QueueEntryRepository

    @Autowired
    lateinit var musicRoomRepository: MusicRoomRepository

    @Autowired
    lateinit var trackRepository: TrackRepository

    @Autowired
    lateinit var transactionManager: PlatformTransactionManager

    private val json = JsonMapper.builder().build()

    @BeforeEach
    fun clean() {
        TransactionTemplate(transactionManager).executeWithoutResult {
            voteRepository.deleteAll()
            voteRepository.flush()
            queueEntryRepository.deleteAll()
            queueEntryRepository.flush()
            musicRoomRepository.deleteAll()
            musicRoomRepository.flush()
            trackRepository.deleteAll()
            trackRepository.flush()
            scoreEntryRepository.deleteAll()
            scoreEntryRepository.flush()
            nightGameRepository.deleteAll()
            nightGameRepository.flush()
            registrationRepository.deleteAll()
            registrationRepository.flush()
            gameNightRepository.deleteAll()
            gameNightRepository.flush()
            boardGameRepository.findAll().forEach { it.tags.clear() }
            boardGameRepository.flush()
            boardGameRepository.deleteAll()
            memberRepository.findAll().forEach { it.seasonPoints = 0 }
            memberRepository.flush()
        }
    }

    @Test
    fun `scores crud recalculates season points and delete requires jwt`() {
        val catanId = saveGame("Catan")
        val azulId = saveGame("Azul")
        val wingspanId = saveGame("Wingspan")
        val lucasId = memberRepository.findByDisplayName("Lucas")!!.id!!
        val anaId = memberRepository.findByDisplayName("Ana")!!.id!!
        val brunoId = memberRepository.findByDisplayName("Bruno")!!.id!!
        val nightId = createNight("Rodada 4")
        val otherNightId = createNight("Rodada 5")

        mockMvc.perform(post("/game-nights/$nightId/games/$catanId")).andExpect(status().isOk)
        mockMvc.perform(post("/game-nights/$nightId/games/$azulId")).andExpect(status().isOk)
        mockMvc.perform(post("/game-nights/$nightId/members/$lucasId")).andExpect(status().isOk)
        mockMvc.perform(post("/game-nights/$nightId/members/$anaId")).andExpect(status().isOk)

        val lucasCatan = mockMvc.perform(
            post("/game-nights/$nightId/scores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoreJson(lucasId, catanId, placement = 1, bonusPoints = 0, notes = "mesa final"))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.placement").value(1))
            .andExpect(jsonPath("$.points").value(10))
            .andExpect(jsonPath("$.seasonPoints").value(10))
            .andExpect(jsonPath("$.displayName").value("Lucas"))
            .andExpect(jsonPath("$.boardGameName").value("Catan"))
            .andExpect(jsonPath("$.notes").value("mesa final"))
            .andReturn()
        val lucasCatanId = json.readTree(lucasCatan.response.contentAsString).get("id").asLong()

        mockMvc.perform(
            post("/game-nights/$nightId/scores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoreJson(lucasId, azulId, placement = 2, bonusPoints = 1))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.points").value(10))
            .andExpect(jsonPath("$.seasonPoints").value(20))

        val anaCatan = mockMvc.perform(
            post("/game-nights/$nightId/scores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoreJson(anaId, catanId, placement = 3, bonusPoints = 0))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.points").value(8))
            .andExpect(jsonPath("$.seasonPoints").value(8))
            .andReturn()
        val anaCatanId = json.readTree(anaCatan.response.contentAsString).get("id").asLong()

        memberRepository.findByDisplayName("Lucas")!!.seasonPoints shouldBe 20
        memberRepository.findByDisplayName("Ana")!!.seasonPoints shouldBe 8

        mockMvc.perform(get("/game-nights/$nightId/scores"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(3)))
            .andExpect(jsonPath("$[0].boardGameName").value("Azul"))
            .andExpect(jsonPath("$[0].displayName").value("Lucas"))
            .andExpect(jsonPath("$[0].points").value(10))
            .andExpect(jsonPath("$[1].boardGameName").value("Catan"))
            .andExpect(jsonPath("$[1].displayName").value("Lucas"))
            .andExpect(jsonPath("$[1].placement").value(1))
            .andExpect(jsonPath("$[2].displayName").value("Ana"))
            .andExpect(jsonPath("$[2].points").value(8))

        mockMvc.perform(get("/game-nights/$nightId/members"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].displayName").value("Ana"))
            .andExpect(jsonPath("$[0].seasonPoints").value(8))
            .andExpect(jsonPath("$[1].displayName").value("Lucas"))
            .andExpect(jsonPath("$[1].seasonPoints").value(20))

        mockMvc.perform(
            put("/game-nights/$nightId/scores/$lucasCatanId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoreJson(lucasId, catanId, placement = 11, bonusPoints = 0))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.points").value(0))
            .andExpect(jsonPath("$.seasonPoints").value(10))

        memberRepository.findByDisplayName("Lucas")!!.seasonPoints shouldBe 10

        mockMvc.perform(
            post("/game-nights/$nightId/scores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoreJson(lucasId, azulId, placement = 1, bonusPoints = 0))
        ).andExpect(status().isBadRequest)

        mockMvc.perform(
            post("/game-nights/$nightId/scores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoreJson(lucasId, catanId, placement = 0, bonusPoints = 0))
        ).andExpect(status().isBadRequest)

        mockMvc.perform(
            post("/game-nights/$nightId/scores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoreJson(brunoId, catanId, placement = 4, bonusPoints = 0))
        ).andExpect(status().isBadRequest)

        mockMvc.perform(
            post("/game-nights/$nightId/scores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoreJson(lucasId, wingspanId, placement = 1, bonusPoints = 0))
        ).andExpect(status().isBadRequest)

        mockMvc.perform(get("/game-nights/999999/scores"))
            .andExpect(status().isNotFound)

        mockMvc.perform(
            put("/game-nights/$otherNightId/scores/$anaCatanId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoreJson(anaId, catanId, placement = 1, bonusPoints = 0))
        ).andExpect(status().isNotFound)

        val azulScoreId = json.readTree(
            mockMvc.perform(get("/game-nights/$nightId/scores"))
                .andExpect(status().isOk)
                .andReturn()
                .response.contentAsString
        ).get(0).get("id").asLong()

        mockMvc.perform(delete("/game-nights/$nightId/scores/$azulScoreId"))
            .andExpect(status().isForbidden)

        val token = loginToken()

        mockMvc.perform(
            delete("/game-nights/$nightId/scores/$azulScoreId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        memberRepository.findByDisplayName("Lucas")!!.seasonPoints shouldBe 0
        memberRepository.findByDisplayName("Ana")!!.seasonPoints shouldBe 8

        mockMvc.perform(get("/game-nights/$nightId/scores"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(2)))

        mockMvc.perform(get("/board-games")).andExpect(status().isOk)
        mockMvc.perform(get("/game-nights")).andExpect(status().isOk)
        mockMvc.perform(get("/tags")).andExpect(status().isOk)
    }

    @Test
    fun `deleting the night or the board game clears the season points of those scores`() {
        val catanId = saveGame("Catan")
        val azulId = saveGame("Azul")
        val lucasId = memberRepository.findByDisplayName("Lucas")!!.id!!
        val nightA = createNight("Noite A")
        val nightB = createNight("Noite B")

        mockMvc.perform(post("/game-nights/$nightA/games/$catanId")).andExpect(status().isOk)
        mockMvc.perform(post("/game-nights/$nightB/games/$azulId")).andExpect(status().isOk)
        mockMvc.perform(post("/game-nights/$nightA/members/$lucasId")).andExpect(status().isOk)
        mockMvc.perform(post("/game-nights/$nightB/members/$lucasId")).andExpect(status().isOk)

        mockMvc.perform(
            post("/game-nights/$nightA/scores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoreJson(lucasId, catanId, placement = 1, bonusPoints = 0))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/game-nights/$nightB/scores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoreJson(lucasId, azulId, placement = 2, bonusPoints = 0))
        ).andExpect(status().isCreated)

        memberRepository.findByDisplayName("Lucas")!!.seasonPoints shouldBe 19

        val token = loginToken()

        mockMvc.perform(
            delete("/game-nights/$nightA")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        memberRepository.findByDisplayName("Lucas")!!.seasonPoints shouldBe 9
        mockMvc.perform(get("/game-nights/$nightA")).andExpect(status().isNotFound)
        mockMvc.perform(get("/game-nights/$nightB/members"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].seasonPoints").value(9))

        mockMvc.perform(
            delete("/board-games/$azulId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        memberRepository.findByDisplayName("Lucas")!!.seasonPoints shouldBe 0
        mockMvc.perform(get("/board-games/$azulId")).andExpect(status().isNotFound)
        mockMvc.perform(get("/board-games/$catanId")).andExpect(status().isOk)
        mockMvc.perform(get("/game-nights/$nightB")).andExpect(status().isOk)
    }

    private fun createNight(title: String): Long {
        val created = mockMvc.perform(
            post("/game-nights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        json.createObjectNode().apply {
                            put("title", title)
                            put("scheduledAt", "2026-10-16T22:00:00Z")
                            put("location", "Sala 1")
                        }
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()
        return json.readTree(created.response.contentAsString).get("id").asLong()
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

    private fun scoreJson(
        memberId: Long,
        boardGameId: Long,
        placement: Int,
        bonusPoints: Int,
        notes: String? = null,
    ): String {
        val node = json.createObjectNode().apply {
            put("memberId", memberId)
            put("boardGameId", boardGameId)
            put("placement", placement)
            put("bonusPoints", bonusPoints)
            if (notes != null) put("notes", notes)
        }
        return json.writeValueAsString(node)
    }
}
