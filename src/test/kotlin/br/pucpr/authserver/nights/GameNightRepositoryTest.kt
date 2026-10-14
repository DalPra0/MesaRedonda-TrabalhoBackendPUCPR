package br.pucpr.authserver.nights

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.Sort
import java.time.Instant

@DataJpaTest
class GameNightRepositoryTest @Autowired constructor(
    val nights: GameNightRepository,
) {
    @Test
    fun `search filters by status and scheduled range and sorts by scheduledAt descending`() {
        nights.save(night("Abertura", "2026-10-02T19:00:00Z", GameNightStatus.DRAFT))
        nights.save(night("Final", "2026-10-20T19:00:00Z", GameNightStatus.OPEN))
        nights.save(night("Cancelada", "2026-10-18T19:00:00Z", GameNightStatus.CANCELLED))

        nights.findAll(
            gameNightSearch(status = null, from = null, to = null),
            Sort.by(Sort.Direction.DESC, "scheduledAt"),
        ).map { it.title }
            .shouldContainExactly("Final", "Cancelada", "Abertura")

        nights.findAll(
            gameNightSearch(status = GameNightStatus.OPEN, from = null, to = null),
            Sort.by(Sort.Direction.DESC, "scheduledAt"),
        ).map { it.title }
            .shouldContainExactly("Final")

        nights.findAll(
            gameNightSearch(
                status = null,
                from = Instant.parse("2026-10-10T00:00:00Z"),
                to = Instant.parse("2026-10-19T00:00:00Z"),
            ),
            Sort.by(Sort.Direction.ASC, "title"),
        ).map { it.title }
            .shouldContainExactly("Cancelada")
    }

    private fun night(title: String, scheduledAt: String, status: GameNightStatus) = GameNight(
        title = title,
        scheduledAt = Instant.parse(scheduledAt),
        location = "Sala 1",
        status = status,
    )
}
