package digdaserver.domain.diary.presentation.dto.res

import digdaserver.domain.comment.domain.entity.Comment
import java.time.LocalDateTime

data class DiaryCommentResponse(
    val id: Long,
    val text: String,
    val createdBy: DiaryUserSummary,
    val createdAt: LocalDateTime,
    /** 대댓글이면 부모 댓글 id. 최상위 댓글은 null. */
    val parentId: Long? = null,
    /** 차단/신고로 숨겨진 댓글인지. true 면 text 는 비워진 상태. */
    val hidden: Boolean = false,
    val hiddenReason: String? = null
) {
    /** 차단/신고로 숨겨야 할 때 — 텍스트를 비운 사본. */
    fun asHidden(reason: String): DiaryCommentResponse = copy(
        text = "",
        hidden = true,
        hiddenReason = reason
    )

    companion object {
        fun from(comment: Comment): DiaryCommentResponse = DiaryCommentResponse(
            id = comment.id,
            text = comment.text,
            createdBy = DiaryUserSummary.from(comment.createdBy),
            createdAt = comment.createdAt,
            parentId = comment.parentId
        )
    }
}
