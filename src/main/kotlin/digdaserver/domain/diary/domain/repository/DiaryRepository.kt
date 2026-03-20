package digdaserver.domain.diary.domain.repository

import digdaserver.domain.diary.domain.entity.Diary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface DiaryRepository : JpaRepository<Diary, Long> {

    fun findAllByGroupId(groupId: Long, pageable: Pageable): Page<Diary>

    @Query("SELECT d FROM Diary d WHERE d.group.id = :groupId AND d.date BETWEEN :startDate AND :endDate ORDER BY d.createdAt DESC")
    fun findAllByGroupIdAndDateBetween(groupId: Long, startDate: LocalDate, endDate: LocalDate, pageable: Pageable): Page<Diary>

    @Query("SELECT DISTINCT d.date FROM Diary d WHERE d.group.id = :groupId AND d.date BETWEEN :startDate AND :endDate")
    fun findDistinctDatesByGroupIdAndMonth(groupId: Long, startDate: LocalDate, endDate: LocalDate): List<LocalDate>
}
