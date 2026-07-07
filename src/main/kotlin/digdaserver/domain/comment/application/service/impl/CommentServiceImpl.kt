package digdaserver.domain.comment.application.service.impl

import digdaserver.domain.comment.application.service.CommentService
import digdaserver.domain.comment.domain.entity.Comment
import digdaserver.domain.comment.domain.entity.CommentTargetType
import digdaserver.domain.comment.domain.repository.CommentRepository
import digdaserver.domain.comment.presentation.dto.res.CreateCommentResponse
import digdaserver.domain.diary.domain.repository.DiaryRepository
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.log.application.service.UserActionLogService
import digdaserver.domain.log.domain.entity.UserAction
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.schedule.domain.repository.ScheduleRepository
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CommentServiceImpl(
    private val commentRepository: CommentRepository,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val scheduleRepository: ScheduleRepository,
    private val diaryRepository: DiaryRepository,
    private val userRepository: UserRepository,
    private val userActionLogService: UserActionLogService,
    private val notificationService: NotificationService
) : CommentService {

    @Transactional
    override fun createScheduleComment(userId: UUID, groupRoomId: Long, scheduleId: Long, text: String): CreateCommentResponse {
        validateGroupRoomMember(groupRoomId, userId)
        validateCommentText(text)

        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { DigdaException(ErrorCode.SCHEDULE_NOT_FOUND) }

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        if (user.restricted) throw DigdaException(ErrorCode.USER_RESTRICTED)

        val comment = commentRepository.save(
            Comment(
                targetType = CommentTargetType.SCHEDULE,
                targetId = scheduleId,
                text = text,
                createdBy = user
            )
        )

        userActionLogService.record(
            actorId = userId,
            action = UserAction.CREATE_COMMENT,
            targetType = "SCHEDULE",
            targetId = scheduleId.toString(),
            detail = "groupRoomId=$groupRoomId, commentId=${comment.id}"
        )

        notificationService.notifyCommentOnSchedule(
            groupRoomId = groupRoomId,
            scheduleId = scheduleId,
            commenterUserId = userId,
            scheduleTitle = schedule.title
        )

        return CreateCommentResponse.from(comment)
    }

    @Transactional
    override fun createDiaryComment(
        userId: UUID,
        groupRoomId: Long,
        diaryId: Long,
        text: String,
        parentCommentId: Long?
    ): CreateCommentResponse {
        validateGroupRoomMember(groupRoomId, userId)
        validateCommentText(text)

        val diary = diaryRepository.findById(diaryId)
            .orElseThrow { DigdaException(ErrorCode.DIARY_NOT_FOUND) }

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        if (user.restricted) throw DigdaException(ErrorCode.USER_RESTRICTED)

        // 대댓글 검증 — 부모가 같은 일기의 최상위 댓글이어야 한다(댓글→대댓글 1단계만 허용).
        if (parentCommentId != null) {
            val parent = commentRepository.findById(parentCommentId)
                .orElseThrow { DigdaException(ErrorCode.COMMENT_NOT_FOUND) }
            if (parent.targetType != CommentTargetType.DIARY || parent.targetId != diaryId) {
                throw DigdaException(ErrorCode.COMMENT_NOT_FOUND)
            }
            if (parent.parentId != null) {
                throw DigdaException(ErrorCode.COMMENT_REPLY_DEPTH_EXCEEDED)
            }
        }

        val comment = commentRepository.save(
            Comment(
                targetType = CommentTargetType.DIARY,
                targetId = diaryId,
                text = text,
                createdBy = user,
                parentId = parentCommentId
            )
        )

        userActionLogService.record(
            actorId = userId,
            action = UserAction.CREATE_COMMENT,
            targetType = "DIARY",
            targetId = diaryId.toString(),
            detail = "groupRoomId=$groupRoomId, commentId=${comment.id}"
        )

        notificationService.notifyCommentOnDiary(
            groupRoomId = groupRoomId,
            diaryId = diaryId,
            commenterUserId = userId,
            diaryTitle = diary.title
        )

        return CreateCommentResponse.from(comment)
    }

    @Transactional
    override fun deleteScheduleComment(userId: UUID, groupRoomId: Long, scheduleId: Long, commentId: Long) {
        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val comment = commentRepository.findById(commentId)
            .orElseThrow { DigdaException(ErrorCode.COMMENT_NOT_FOUND) }

        if (comment.createdBy.id != userId && !membership.isOwner) {
            throw DigdaException(ErrorCode.FORBIDDEN)
        }

        commentRepository.delete(comment)
    }

    @Transactional
    override fun deleteDiaryComment(userId: UUID, groupRoomId: Long, diaryId: Long, commentId: Long) {
        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val comment = commentRepository.findById(commentId)
            .orElseThrow { DigdaException(ErrorCode.COMMENT_NOT_FOUND) }

        if (comment.createdBy.id != userId && !membership.isOwner) {
            throw DigdaException(ErrorCode.FORBIDDEN)
        }

        // 최상위 댓글을 지우면 매달린 대댓글도 함께 삭제(고아 방지 — FK 없이 parent_comment_id 로 매핑).
        if (comment.parentId == null) {
            commentRepository.deleteAllByParentId(comment.id)
        }
        commentRepository.delete(comment)
    }

    private fun validateGroupRoomMember(groupRoomId: Long, userId: UUID) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }
    }

    private fun validateCommentText(text: String) {
        if (text.length > 200) throw DigdaException(ErrorCode.COMMENT_TOO_LONG)
    }
}
