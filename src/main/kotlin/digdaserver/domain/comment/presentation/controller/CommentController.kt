package digdaserver.domain.comment.presentation.controller

import digdaserver.domain.comment.application.service.CommentService
import digdaserver.domain.comment.presentation.dto.req.CreateCommentRequest
import digdaserver.domain.comment.presentation.dto.res.CreateCommentResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Tag(name = "Comment", description = "댓글 API")
class CommentController(
    private val commentService: CommentService
) {

    @Operation(summary = "일정 댓글 작성", description = "일정에 댓글을 작성합니다.")
    @PostMapping("/group-rooms/{groupRoomId}/schedules/{scheduleId}/comments")
    fun createScheduleComment(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable scheduleId: Long,
        @RequestBody request: CreateCommentRequest
    ): ResponseEntity<CreateCommentResponse> {
        val response = commentService.createScheduleComment(UUID.fromString(userId), groupRoomId, scheduleId, request.text)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "일기 댓글 작성", description = "일기에 댓글을 작성합니다.")
    @PostMapping("/group-rooms/{groupRoomId}/diaries/{diaryId}/comments")
    fun createDiaryComment(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable diaryId: Long,
        @RequestBody request: CreateCommentRequest
    ): ResponseEntity<CreateCommentResponse> {
        val response = commentService.createDiaryComment(UUID.fromString(userId), groupRoomId, diaryId, request.text)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "일정 댓글 삭제", description = "일정 댓글을 삭제합니다. 작성자 또는 방장만 가능합니다.")
    @DeleteMapping("/group-rooms/{groupRoomId}/schedules/{scheduleId}/comments/{commentId}")
    fun deleteScheduleComment(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable scheduleId: Long,
        @PathVariable commentId: Long
    ): ResponseEntity<Void> {
        commentService.deleteScheduleComment(UUID.fromString(userId), groupRoomId, scheduleId, commentId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "일기 댓글 삭제", description = "일기 댓글을 삭제합니다. 작성자 또는 방장만 가능합니다.")
    @DeleteMapping("/group-rooms/{groupRoomId}/diaries/{diaryId}/comments/{commentId}")
    fun deleteDiaryComment(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable diaryId: Long,
        @PathVariable commentId: Long
    ): ResponseEntity<Void> {
        commentService.deleteDiaryComment(UUID.fromString(userId), groupRoomId, diaryId, commentId)
        return ResponseEntity.noContent().build()
    }
}
