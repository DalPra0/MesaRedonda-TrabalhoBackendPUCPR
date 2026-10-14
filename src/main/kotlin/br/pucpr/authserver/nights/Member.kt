package br.pucpr.authserver.nights

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
class Member(
    @Id @GeneratedValue
    var id: Long? = null,

    @Column(unique = true, nullable = false)
    var displayName: String,

    @Column
    var email: String? = null,

    @Column(nullable = false)
    var seasonPoints: Int = 0,
)
