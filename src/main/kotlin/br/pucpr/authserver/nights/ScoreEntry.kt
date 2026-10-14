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

/**
 * Uma linha do placar de uma noite: quem ficou em qual posição, em qual jogo.
 *
 * Os pontos da linha seguem [scorePoints]. [Member.seasonPoints] é a soma
 * dessas linhas na temporada e é recalculado quando um placar é criado,
 * atualizado ou apagado.
 */
@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["idGameNight", "idMember", "idBoardGame"]),
    ]
)
class ScoreEntry(
    @Id @GeneratedValue
    var id: Long? = null,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idGameNight", nullable = false)
    var gameNight: GameNight,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idMember", nullable = false)
    var member: Member,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idBoardGame", nullable = false)
    var boardGame: BoardGame,

    @Column(nullable = false)
    var placement: Int,

    @Column(nullable = false)
    var bonusPoints: Int = 0,

    @Column
    var notes: String? = null,
)

/**
 * Pontos de uma linha do placar da temporada.
 *
 * `max(0, 11 - placement) + bonusPoints`.
 *
 * A colocação 1 vale 10, a 2 vale 9, até a 10, que vale 1.
 * Da colocação 11 em diante a parte da colocação vale 0.
 * [bonusPoints] entra na soma como está e não pode ser negativo.
 * [Member.seasonPoints] é a soma desta conta em todos os [ScoreEntry] do membro.
 */
fun scorePoints(placement: Int, bonusPoints: Int): Int =
    maxOf(0, 11 - placement) + bonusPoints
