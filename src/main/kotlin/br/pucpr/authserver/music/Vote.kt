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
 * Um voto por membro em cada entrada da fila.
 * Votar de novo troca o [type], não cria outra linha.
 */
@Entity
@Table(
    name = "MusicVote",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["idQueueEntry", "idMember"]),
    ]
)
class Vote(
    @Id @GeneratedValue
    var id: Long? = null,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idQueueEntry", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var queueEntry: QueueEntry,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idMember", nullable = false)
    var member: Member,

    @Enumerated(EnumType.STRING)
    @Column(name = "voteType", nullable = false)
    var type: VoteType,
)
