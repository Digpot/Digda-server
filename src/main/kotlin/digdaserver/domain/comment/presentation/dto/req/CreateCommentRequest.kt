package digdaserver.domain.comment.presentation.dto.req

data class CreateCommentRequest(
    val text: String,
    /** 대댓글이면 부모 댓글 id. null 이면 최상위 댓글. 대댓글의 대댓글(2단계)은 거부된다. */
    val parentCommentId: Long? = null
)
