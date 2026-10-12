package br.pucpr.authserver.music

import br.pucpr.authserver.nights.GameNight
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

/**
 * Sala PulseRoom de uma noite. Uma [GameNight] tem no máximo uma sala.
 */
@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["idGameNight"]),
        UniqueConstraint(columnNames = ["inviteCode"]),
    ]
)
class MusicRoom(
    @Id @GeneratedValue
    var id: Long? = null,

    @OneToOne(optional = false)
    @JoinColumn(name = "idGameNight", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var gameNight: GameNight,

    @Column(nullable = false)
    var inviteCode: String,

    @Column
    var mood: String? = null,

    @Column(nullable = false)
    var active: Boolean = true,
)
