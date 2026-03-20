package digdaserver.domain.comment.domain.repository

import digdaserver.domain.comment.domain.entity.Comment
import digdaserver.domain.comment.domain.entity.CommentTargetType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {

    fun findAllByTargetTypeAndTargetIdOrderByCreatedAtAsc(
        targetType: CommentTargetType,
        targetId: Long
    ): List<Comment>

    fun countByTargetTypeAndTargetId(targetType: CommentTargetType, targetId: Long): Int
}
