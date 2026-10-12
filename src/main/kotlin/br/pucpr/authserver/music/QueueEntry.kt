package br.pucpr.authserver.music

import br.pucpr.authserver.nights.Member
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

/**
 * Uma música na fila da sala.
 *
 * [position] é a ordem de chegada: a nova entrada fica no fim
 * (`max(position) + 1`), inclusive depois de REMOVED, DONE e SKIPPED.
 * Remover não apaga a linha e não recompacta a posição.
 */
@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["idMusicRoom", "position"]),
    ]
)
class QueueEntry(
    @Id @GeneratedValue
    var id: Long? = null,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idMusicRoom", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var musicRoom: MusicRoom,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idTrack", nullable = false)
    var track: Track,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idMember", nullable = false)
    var requestedBy: Member,

    @Column(nullable = false)
    var position: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: QueueStatus = QueueStatus.QUEUED,
)
