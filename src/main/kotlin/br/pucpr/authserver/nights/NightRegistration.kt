package br.pucpr.authserver.nights

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

@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["idGameNight", "idMember"]),
    ]
)
class NightRegistration(
    @Id @GeneratedValue
    var id: Long? = null,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idGameNight", nullable = false)
    var night: GameNight,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idMember", nullable = false)
    var member: Member,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: NightRole = NightRole.PLAYER,
)
