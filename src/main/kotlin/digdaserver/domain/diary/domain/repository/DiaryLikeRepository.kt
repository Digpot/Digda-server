package digdaserver.domain.diary.domain.repository

import digdaserver.domain.diary.domain.entity.DiaryLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DiaryLikeRepository : JpaRepository<DiaryLike, Long> {

    fun existsByDiaryIdAndUserId(diaryId: Long, userId: UUID): Boolean

    fun countByDiaryId(diaryId: Long): Long

    @Modifying
    @Query("DELETE FROM DiaryLike dl WHERE dl.diary.id = :diaryId AND dl.user.id = :userId")
    fun deleteByDiaryIdAndUserId(@Param("diaryId") diaryId: Long, @Param("userId") userId: UUID): Int

    @Query(
        """
        SELECT dl.diary.id, COUNT(dl)
        FROM DiaryLike dl
        WHERE dl.diary.id IN :diaryIds
        GROUP BY dl.diary.id
        """
    )
    fun countByDiaryIdIn(@Param("diaryIds") diaryIds: List<Long>): List<Array<Any>>

    @Query(
        """
        SELECT dl.diary.id
        FROM DiaryLike dl
        WHERE dl.diary.id IN :diaryIds AND dl.user.id = :userId
        """
    )
    fun findLikedDiaryIds(
        @Param("diaryIds") diaryIds: List<Long>,
        @Param("userId") userId: UUID
    ): List<Long>

    @Modifying
    @Query("DELETE FROM DiaryLike dl WHERE dl.user.id = :userId")
    fun deleteAllByUserId(@Param("userId") userId: UUID)
}
