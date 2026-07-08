package digdaserver.domain.comment.application.service

import digdaserver.domain.comment.presentation.dto.res.CreateCommentResponse
import java.util.UUID

interface CommentService {

    fun createScheduleComment(userId: UUID, groupRoomId: Long, scheduleId: Long, text: String): CreateCommentResponse

    fun createDiaryComment(
        userId: UUID,
        groupRoomId: Long,
        diaryId: Long,
        text: String,
        parentCommentId: Long? = null
    ): CreateCommentResponse

    fun deleteScheduleComment(userId: UUID, groupRoomId: Long, scheduleId: Long, commentId: Long)

    fun deleteDiaryComment(userId: UUID, groupRoomId: Long, diaryId: Long, commentId: Long)
}
