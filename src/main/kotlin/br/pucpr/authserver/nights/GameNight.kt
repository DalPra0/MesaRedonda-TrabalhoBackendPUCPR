package br.pucpr.authserver.nights

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import java.time.Instant

@Entity
class GameNight(
    @Id @GeneratedValue
    var id: Long? = null,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false)
    var scheduledAt: Instant,

    @Column(nullable = false)
    var location: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: GameNightStatus = GameNightStatus.DRAFT,
)
