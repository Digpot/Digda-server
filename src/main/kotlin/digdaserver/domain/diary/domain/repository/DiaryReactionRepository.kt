package digdaserver.domain.diary.domain.repository

import digdaserver.domain.diary.domain.entity.DiaryReaction
import digdaserver.domain.diary.domain.entity.DiaryReactionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DiaryReactionRepository : JpaRepository<DiaryReaction, Long> {

    fun existsByDiaryIdAndUserIdAndType(diaryId: Long, userId: UUID, type: DiaryReactionType): Boolean

    @Modifying
    @Query(
        """
        DELETE FROM DiaryReaction dr
        WHERE dr.diary.id = :diaryId AND dr.user.id = :userId AND dr.type = :type
        """
    )
    fun deleteOne(
        @Param("diaryId") diaryId: Long,
        @Param("userId") userId: UUID,
        @Param("type") type: DiaryReactionType
    ): Int

    fun findAllByDiaryId(diaryId: Long): List<DiaryReaction>

    @Query(
        """
        SELECT dr.diary.id, dr.type, COUNT(dr)
        FROM DiaryReaction dr
        WHERE dr.diary.id IN :diaryIds
        GROUP BY dr.diary.id, dr.type
        """
    )
    fun countGroupedByDiaryAndType(@Param("diaryIds") diaryIds: List<Long>): List<Array<Any>>

    @Query(
        """
        SELECT dr.diary.id, dr.type
        FROM DiaryReaction dr
        WHERE dr.diary.id IN :diaryIds AND dr.user.id = :userId
        """
    )
    fun findMyReactions(
        @Param("diaryIds") diaryIds: List<Long>,
        @Param("userId") userId: UUID
    ): List<Array<Any>>

    @Modifying
    @Query("DELETE FROM DiaryReaction dr WHERE dr.user.id = :userId")
    fun deleteAllByUserId(@Param("userId") userId: UUID)

    @Modifying
    @Query("DELETE FROM DiaryReaction dr WHERE dr.diary.id = :diaryId")
    fun deleteAllByDiaryId(@Param("diaryId") diaryId: Long): Int
}
