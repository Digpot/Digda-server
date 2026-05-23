package digdaserver.domain.diary.domain.repository

import digdaserver.domain.diary.domain.entity.Diary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface DiaryRepository : JpaRepository<Diary, Long> {

    fun findAllByGroupRoomId(groupRoomId: Long, pageable: Pageable): Page<Diary>

    @Query("SELECT d FROM Diary d WHERE d.groupRoom.id = :groupRoomId AND d.date BETWEEN :startDate AND :endDate ORDER BY d.createdAt DESC")
    fun findAllByGroupRoomIdAndDateBetween(groupRoomId: Long, startDate: LocalDate, endDate: LocalDate, pageable: Pageable): Page<Diary>

    @Query("SELECT DISTINCT d.date FROM Diary d WHERE d.groupRoom.id = :groupRoomId AND d.date BETWEEN :startDate AND :endDate")
    fun findDistinctDatesByGroupRoomIdAndMonth(groupRoomId: Long, startDate: LocalDate, endDate: LocalDate): List<LocalDate>

    @Query(
        """
        SELECT d FROM Diary d
        WHERE (:keyword IS NULL OR :keyword = ''
            OR LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(d.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """
    )
    fun searchForAdmin(
        @Param("keyword") keyword: String?,
        pageable: Pageable
    ): Page<Diary>

    @Modifying
    @Query("DELETE FROM Diary d WHERE d.createdBy.id = :userId")
    fun deleteAllByCreatedById(@Param("userId") userId: UUID)
}
