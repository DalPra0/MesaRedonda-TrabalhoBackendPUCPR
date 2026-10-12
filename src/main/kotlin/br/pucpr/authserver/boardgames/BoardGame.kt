package br.pucpr.authserver.boardgames

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany

@Entity
class BoardGame(
    @Id @GeneratedValue
    var id: Long? = null,

    @Column(unique = true, nullable = false)
    var name: String,

    @Column(nullable = false)
    var minPlayers: Int,

    @Column(nullable = false)
    var maxPlayers: Int,

    @Column(nullable = false)
    var durationMinutes: Int,

    @Column(nullable = false)
    var weight: Int,

    @Column(nullable = false)
    var description: String = "",

    @ManyToMany
    @JoinTable(
        name = "BoardGameTag",
        joinColumns = [JoinColumn(name = "idBoardGame")],
        inverseJoinColumns = [JoinColumn(name = "idTag")]
    )
    var tags: MutableSet<Tag> = mutableSetOf()
)
