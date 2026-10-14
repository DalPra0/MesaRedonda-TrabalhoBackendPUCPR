package br.pucpr.authserver.music

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TrackRepository : JpaRepository<Track, Long> {
    @Query(
        """
            select t from Track t
            where (:artist is null or lower(t.artist) like lower(concat('%', :artist, '%')))
              and (:title is null or lower(t.title) like lower(concat('%', :title, '%')))
        """
    )
    fun search(artist: String?, title: String?, sort: Sort): List<Track>
}
