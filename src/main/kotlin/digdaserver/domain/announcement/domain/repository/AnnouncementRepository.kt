package digdaserver.domain.announcement.domain.repository

import digdaserver.domain.announcement.domain.entity.Announcement
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AnnouncementRepository : JpaRepository<Announcement, Long> {

    @Query(
        """
        SELECT a FROM Announcement a
        WHERE (:keyword IS NULL OR a.title LIKE %:keyword% OR a.body LIKE %:keyword%)
        """
    )
    fun searchForAdmin(@Param("keyword") keyword: String?, pageable: Pageable): Page<Announcement>
}
