package digdaserver.domain.oauth2.application.service.impl.auth

import digdaserver.domain.comment.domain.repository.CommentRepository
import digdaserver.domain.device.domain.repository.DeviceRepository
import digdaserver.domain.diary.domain.repository.DiaryRepository
import digdaserver.domain.group_room.domain.entity.GroupRoomRole
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.domain.repository.NotificationRepository
import digdaserver.domain.oauth2.application.service.AccountService
import digdaserver.domain.schedule.domain.repository.ScheduleParticipantRepository
import digdaserver.domain.schedule.domain.repository.ScheduleRepository
import digdaserver.domain.todo.domain.repository.TodoRepository
import digdaserver.domain.upload.domain.repository.UploadedImageRepository
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.jwt.domain.repository.JsonWebTokenRepository
import digdaserver.global.jwt.domain.repository.SocialTokenRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Transactional
class AccountServiceImpl(
    private val userRepository: UserRepository,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val jsonWebTokenRepository: JsonWebTokenRepository,
    private val socialTokenRepository: SocialTokenRepository,
    private val scheduleParticipantRepository: ScheduleParticipantRepository,
    private val scheduleRepository: ScheduleRepository,
    private val commentRepository: CommentRepository,
    private val diaryRepository: DiaryRepository,
    private val todoRepository: TodoRepository,
    private val notificationRepository: NotificationRepository,
    private val deviceRepository: DeviceRepository,
    private val uploadedImageRepository: UploadedImageRepository
) : AccountService {

    private val log = LoggerFactory.getLogger(AccountServiceImpl::class.java)

    override fun deleteAccount(userId: UUID) {
        log.info("userId={}, action=회원 탈퇴 요청", userId)

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val memberships = membershipRepository.findAllByUserIdWithGroupRoom(userId)
        val ownsActiveGroup = memberships.any { it.role == GroupRoomRole.OWNER }

        if (ownsActiveGroup) {
            throw DigdaException(ErrorCode.OWNS_ACTIVE_GROUP_ROOM)
        }

        // 토큰 무효화
        jsonWebTokenRepository.deleteByProviderId(userId.toString())
        socialTokenRepository.deleteByUserId(userId.toString())

        // 다른 사용자 todo 의 completedBy 참조 null 처리 (nullable FK)
        todoRepository.clearCompletedByUserId(userId)

        // 일정 참여 기록 제거 (다른 사람 일정 참여 + 내 일정의 다른 참여자)
        scheduleParticipantRepository.deleteAllByUserId(userId)
        scheduleParticipantRepository.deleteAllByScheduleCreatedById(userId)

        // 내가 만든 컨텐츠 삭제
        scheduleRepository.deleteAllByCreatedById(userId)
        commentRepository.deleteAllByCreatedById(userId)
        diaryRepository.deleteAllByCreatedById(userId)
        todoRepository.deleteAllByCreatedById(userId)

        // 알림·기기·이미지 삭제
        notificationRepository.deleteAllByUserId(userId)
        deviceRepository.deleteAllByUserId(userId)
        uploadedImageRepository.deleteAllByUserId(userId)

        // 멤버십 삭제 및 빈 그룹 정리
        val groupRoomIds = memberships.map { it.groupRoom.id!! }
        memberships.forEach { membership ->
            membershipRepository.delete(membership)
        }
        membershipRepository.flush()

        // 남은 멤버가 0명인 그룹 삭제 (cascade로 일기, 일정, 할일 등 모두 삭제)
        groupRoomIds.forEach { groupRoomId ->
            if (membershipRepository.countByGroupRoomId(groupRoomId) == 0) {
                groupRoomRepository.deleteById(groupRoomId)
                log.info("빈 그룹방 삭제: groupRoomId={}", groupRoomId)
            }
        }

        // 계정 삭제
        userRepository.delete(user)

        log.info("회원 탈퇴 완료: userId={}", userId)
    }
}
