package digdaserver.domain.comment.domain.repository

import digdaserver.domain.comment.domain.entity.Comment
import digdaserver.domain.comment.domain.entity.CommentTargetType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {

    fun findAllByTargetTypeAndTargetIdOrderByCreatedAtAsc(
        targetType: CommentTargetType,
        targetId: Long
    ): List<Comment>

    fun countByTargetTypeAndTargetId(targetType: CommentTargetType, targetId: Long): Int

    @Query(
        """
        SELECT c.targetId, COUNT(c)
        FROM Comment c
        WHERE c.targetType = :targetType AND c.targetId IN :targetIds
        GROUP BY c.targetId
        """
    )
    fun countByTargetTypeAndTargetIdIn(
        @Param("targetType") targetType: CommentTargetType,
        @Param("targetIds") targetIds: Collection<Long>
    ): List<Array<Any>>
}
