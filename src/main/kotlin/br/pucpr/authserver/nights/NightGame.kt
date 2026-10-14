package br.pucpr.authserver.nights

import br.pucpr.authserver.boardgames.BoardGame
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["idGameNight", "idBoardGame"]),
        UniqueConstraint(columnNames = ["idGameNight", "draftOrder"]),
    ]
)
class NightGame(
    @Id @GeneratedValue
    var id: Long? = null,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idGameNight", nullable = false)
    var night: GameNight,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idBoardGame", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var game: BoardGame,

    @Column(name = "draftOrder", nullable = false)
    var draftOrder: Int,

    @Column(nullable = false)
    var confirmed: Boolean = false,
)
