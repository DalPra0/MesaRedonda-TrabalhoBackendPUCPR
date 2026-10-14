package br.pucpr.authserver.music

import br.pucpr.authserver.boardgames.BoardGame
import br.pucpr.authserver.boardgames.BoardGameRepository
import br.pucpr.authserver.nights.GameNightRepository
import br.pucpr.authserver.nights.MemberRepository
import br.pucpr.authserver.nights.NightGameRepository
import br.pucpr.authserver.nights.NightRegistrationRepository
import br.pucpr.authserver.nights.ScoreEntryRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.nullValue
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
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@AutoConfigureMockMvc
class MusicWebTest {
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
    fun `tracks music room and queue follow the end of the line and one vote per member`() {
        val alphaId = createTrack("Alpha", "Zara", 100, "spotify:alpha")
        val meioId = createTrack("Meio", "Ana", 180, null)
        val zebraId = createTrack("Zebra", "Ana", 200, "  ")

        mockMvc.perform(get("/tracks/$alphaId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Alpha"))
            .andExpect(jsonPath("$.artist").value("Zara"))
            .andExpect(jsonPath("$.durationSeconds").value(100))
            .andExpect(jsonPath("$.externalId").value("spotify:alpha"))

        mockMvc.perform(get("/tracks/$zebraId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.externalId").value(nullValue()))

        mockMvc.perform(get("/tracks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(3)))
            .andExpect(jsonPath("$[0].title").value("Alpha"))
            .andExpect(jsonPath("$[1].title").value("Meio"))
            .andExpect(jsonPath("$[2].title").value("Zebra"))

        mockMvc.perform(get("/tracks").param("artist", "ana").param("sort", "durationSeconds,desc"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(2)))
            .andExpect(jsonPath("$[0].title").value("Zebra"))
            .andExpect(jsonPath("$[1].title").value("Meio"))

        mockMvc.perform(get("/tracks").param("title", "alp").param("sort", "artist,desc"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(1)))
            .andExpect(jsonPath("$[0].title").value("Alpha"))

        mockMvc.perform(get("/tracks").param("sort", "nope"))
            .andExpect(status().isBadRequest)

        mockMvc.perform(get("/tracks/999999"))
            .andExpect(status().isNotFound)

        mockMvc.perform(
            post("/tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Curta","artist":"Ninguem","durationSeconds":0}""")
        ).andExpect(status().isBadRequest)

        mockMvc.perform(delete("/tracks/$meioId"))
            .andExpect(status().isForbidden)

        val lucasId = memberRepository.findByDisplayName("Lucas")!!.id!!
        val anaId = memberRepository.findByDisplayName("Ana")!!.id!!
        val nightId = createNight("Rodada com som")
        val otherNightId = createNight("Outra rodada")

        mockMvc.perform(get("/game-nights/$nightId/music-room"))
            .andExpect(status().isNotFound)

        mockMvc.perform(post("/game-nights/$nightId/music-room"))
            .andExpect(status().isForbidden)

        val token = loginToken()

        val createdRoom = mockMvc.perform(
            post("/game-nights/$nightId/music-room")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mood":"chill","inviteCode":"PULSE1","active":true}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.nightId").value(nightId))
            .andExpect(jsonPath("$.inviteCode").value("PULSE1"))
            .andExpect(jsonPath("$.mood").value("chill"))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn()
        val roomId = json.readTree(createdRoom.response.contentAsString).get("id").asLong()

        mockMvc.perform(
            post("/game-nights/$nightId/music-room")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mood":"loud","inviteCode":"OTHER1","active":false}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(roomId))
            .andExpect(jsonPath("$.inviteCode").value("PULSE1"))
            .andExpect(jsonPath("$.mood").value("chill"))
            .andExpect(jsonPath("$.active").value(true))

        mockMvc.perform(get("/game-nights/$nightId/music-room"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(roomId))

        mockMvc.perform(
            post("/game-nights/$otherNightId/music-room")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"PULSE1"}""")
        ).andExpect(status().isBadRequest)

        val generated = mockMvc.perform(
            post("/game-nights/$otherNightId/music-room")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.mood").value(nullValue()))
            .andReturn()
        json.readTree(generated.response.contentAsString).get("inviteCode").stringValue() shouldMatch Regex("[A-Z2-9]{6}")

        mockMvc.perform(
            post("/game-nights/999999/queue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(queueJson(alphaId, lucasId))
        ).andExpect(status().isNotFound)

        val quietNightId = createNight("Sem sala")
        mockMvc.perform(
            post("/game-nights/$quietNightId/queue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(queueJson(alphaId, lucasId))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Music room not found. Create the music room before using the queue"))

        mockMvc.perform(
            post("/game-nights/$nightId/queue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(queueJson(999999L, lucasId))
        ).andExpect(status().isNotFound)

        mockMvc.perform(
            post("/game-nights/$nightId/queue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(queueJson(alphaId, 999999L))
        ).andExpect(status().isNotFound)

        val first = enqueue(nightId, alphaId, lucasId)
        first.get("position").asInt() shouldBe 1
        first.get("status").stringValue() shouldBe "QUEUED"
        val firstId = first.get("id").asLong()

        val second = enqueue(nightId, meioId, lucasId)
        second.get("position").asInt() shouldBe 2
        val secondId = second.get("id").asLong()

        val third = enqueue(nightId, alphaId, anaId)
        third.get("position").asInt() shouldBe 3
        third.get("displayName").stringValue() shouldBe "Ana"
        val thirdId = third.get("id").asLong()

        mockMvc.perform(get("/game-nights/$nightId/queue"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(3)))
            .andExpect(jsonPath("$[0].position").value(1))
            .andExpect(jsonPath("$[0].title").value("Alpha"))
            .andExpect(jsonPath("$[1].position").value(2))
            .andExpect(jsonPath("$[1].title").value("Meio"))
            .andExpect(jsonPath("$[2].position").value(3))
            .andExpect(jsonPath("$[2].displayName").value("Ana"))

        mockMvc.perform(get("/game-nights/$nightId/queue").param("sort", "position,desc"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].position").value(3))
            .andExpect(jsonPath("$[2].position").value(1))

        mockMvc.perform(get("/game-nights/$nightId/queue").param("status", "QUEUED"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(3)))

        mockMvc.perform(get("/game-nights/$nightId/queue").param("sort", "nope"))
            .andExpect(status().isBadRequest)

        mockMvc.perform(post("/game-nights/$nightId/queue/$firstId/play"))
            .andExpect(status().isForbidden)

        mockMvc.perform(
            post("/game-nights/$nightId/queue/$firstId/play")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PLAYING"))

        mockMvc.perform(
            post("/game-nights/$nightId/queue/$secondId/play")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PLAYING"))
            .andExpect(jsonPath("$.id").value(secondId))

        mockMvc.perform(get("/game-nights/$nightId/queue").param("status", "DONE"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Collection<Any>>(1)))
            .andExpect(jsonPath("$[0].id").value(firstId))

        mockMvc.perform(get("/game-nights/$nightId/queue").param("status", "PLAYING"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(secondId))

        mockMvc.perform(
            post("/game-nights/$nightId/queue/$thirdId/skip")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SKIPPED"))

        mockMvc.perform(get("/game-nights/$nightId/queue").param("status", "PLAYING"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(secondId))

        mockMvc.perform(
            post("/game-nights/$nightId/queue/$secondId/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"UP","memberId":$lucasId}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.type").value("UP"))
            .andExpect(jsonPath("$.displayName").value("Lucas"))

        mockMvc.perform(
            post("/game-nights/$nightId/queue/$secondId/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"DOWN","memberId":$lucasId}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("DOWN"))

        mockMvc.perform(
            post("/game-nights/$nightId/queue/$secondId/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"SKIP_REQUEST","memberId":$anaId}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.type").value("SKIP_REQUEST"))

        mockMvc.perform(get("/game-nights/$nightId/queue").param("status", "PLAYING"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].upVotes").value(0))
            .andExpect(jsonPath("$[0].downVotes").value(1))
            .andExpect(jsonPath("$[0].skipRequests").value(1))

        mockMvc.perform(delete("/game-nights/$nightId/queue/$secondId"))
            .andExpect(status().isForbidden)

        mockMvc.perform(
            delete("/game-nights/$nightId/queue/$secondId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        mockMvc.perform(get("/game-nights/$nightId/queue").param("status", "REMOVED"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(secondId))
            .andExpect(jsonPath("$[0].position").value(2))

        mockMvc.perform(
            post("/game-nights/$nightId/queue/$secondId/play")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isBadRequest)

        val fourth = enqueue(nightId, zebraId, anaId)
        fourth.get("position").asInt() shouldBe 4

        mockMvc.perform(
            delete("/tracks/$alphaId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isBadRequest)

        mockMvc.perform(delete("/game-nights/$nightId/music-room"))
            .andExpect(status().isForbidden)

        mockMvc.perform(
            delete("/game-nights/$nightId/music-room")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        mockMvc.perform(get("/game-nights/$nightId/music-room"))
            .andExpect(status().isNotFound)

        mockMvc.perform(
            post("/game-nights/$nightId/queue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(queueJson(alphaId, lucasId))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Music room not found. Create the music room before using the queue"))

        mockMvc.perform(
            delete("/tracks/$alphaId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        mockMvc.perform(get("/tracks/$alphaId"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/board-games")).andExpect(status().isOk)
        mockMvc.perform(get("/game-nights")).andExpect(status().isOk)
        mockMvc.perform(get("/tags")).andExpect(status().isOk)
        mockMvc.perform(get("/game-nights/$otherNightId/music-room")).andExpect(status().isOk)
    }

    @Test
    fun `deleting the night removes the music room and still clears season points`() {
        val catanId = saveGame("Catan")
        val lucasId = memberRepository.findByDisplayName("Lucas")!!.id!!
        val nightId = createNight("Noite A")
        val trackId = createTrack("Tema", "Banda", 90, null)
        val token = loginToken()

        mockMvc.perform(post("/game-nights/$nightId/games/$catanId")).andExpect(status().isOk)
        mockMvc.perform(post("/game-nights/$nightId/members/$lucasId")).andExpect(status().isOk)
        mockMvc.perform(
            post("/game-nights/$nightId/scores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"memberId":$lucasId,"boardGameId":$catanId,"placement":1,"bonusPoints":0}"""
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.points").value(10))
            .andExpect(jsonPath("$.seasonPoints").value(10))

        mockMvc.perform(
            post("/game-nights/$nightId/music-room")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mood":"final"}""")
        ).andExpect(status().isCreated)

        val queued = enqueue(nightId, trackId, lucasId)
        mockMvc.perform(
            post("/game-nights/$nightId/queue/${queued.get("id").asLong()}/play")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)

        memberRepository.findByDisplayName("Lucas")!!.seasonPoints shouldBe 10

        mockMvc.perform(
            delete("/game-nights/$nightId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        memberRepository.findByDisplayName("Lucas")!!.seasonPoints shouldBe 0
        mockMvc.perform(get("/game-nights/$nightId")).andExpect(status().isNotFound)
        mockMvc.perform(get("/game-nights/$nightId/music-room")).andExpect(status().isNotFound)
        mockMvc.perform(get("/game-nights/$nightId/scores")).andExpect(status().isNotFound)
        mockMvc.perform(get("/game-nights/$nightId/queue")).andExpect(status().isNotFound)
        mockMvc.perform(get("/tracks/$trackId")).andExpect(status().isOk)
        mockMvc.perform(get("/board-games/$catanId")).andExpect(status().isOk)
        mockMvc.perform(
            post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"admin@authserver.com","password":"admin"}""")
        ).andExpect(status().isOk)
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

    private fun createTrack(title: String, artist: String, durationSeconds: Int, externalId: String?): Long {
        val node = json.createObjectNode().apply {
            put("title", title)
            put("artist", artist)
            put("durationSeconds", durationSeconds)
            if (externalId != null) put("externalId", externalId)
        }
        val created = mockMvc.perform(
            post("/tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(node))
        )
            .andExpect(status().isCreated)
            .andReturn()
        return json.readTree(created.response.contentAsString).get("id").asLong()
    }

    private fun enqueue(nightId: Long, trackId: Long, memberId: Long) =
        json.readTree(
            mockMvc.perform(
                post("/game-nights/$nightId/queue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(queueJson(trackId, memberId))
            )
                .andExpect(status().isCreated)
                .andReturn()
                .response.contentAsString
        )

    private fun queueJson(trackId: Long, memberId: Long): String =
        json.writeValueAsString(
            json.createObjectNode().apply {
                put("trackId", trackId)
                put("memberId", memberId)
            }
        )

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
}
