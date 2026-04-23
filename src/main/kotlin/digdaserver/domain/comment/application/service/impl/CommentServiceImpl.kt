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
    private val userActionLogService: UserActionLogService
) : CommentService {

    @Transactional
    override fun createScheduleComment(userId: UUID, groupRoomId: Long, scheduleId: Long, text: String): CreateCommentResponse {
        validateGroupRoomMember(groupRoomId, userId)
        validateCommentText(text)

        scheduleRepository.findById(scheduleId)
            .orElseThrow { DigdaException(ErrorCode.SCHEDULE_NOT_FOUND) }

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

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

        return CreateCommentResponse.from(comment)
    }

    @Transactional
    override fun createDiaryComment(userId: UUID, groupRoomId: Long, diaryId: Long, text: String): CreateCommentResponse {
        validateGroupRoomMember(groupRoomId, userId)
        validateCommentText(text)

        diaryRepository.findById(diaryId)
            .orElseThrow { DigdaException(ErrorCode.DIARY_NOT_FOUND) }

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val comment = commentRepository.save(
            Comment(
                targetType = CommentTargetType.DIARY,
                targetId = diaryId,
                text = text,
                createdBy = user
            )
        )

        userActionLogService.record(
            actorId = userId,
            action = UserAction.CREATE_COMMENT,
            targetType = "DIARY",
            targetId = diaryId.toString(),
            detail = "groupRoomId=$groupRoomId, commentId=${comment.id}"
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
