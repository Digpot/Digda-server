package digdaserver.domain.oauth2.application.service.impl.auth

import digdaserver.domain.comment.domain.entity.CommentTargetType
import digdaserver.domain.comment.domain.repository.CommentRepository
import digdaserver.domain.device.domain.repository.DeviceRepository
import digdaserver.domain.diary.domain.repository.DiaryLikeRepository
import digdaserver.domain.diary.domain.repository.DiaryReactionRepository
import digdaserver.domain.diary.domain.repository.DiaryRepository
import digdaserver.domain.group_room.application.scheduler.GroupRoomChildPurger
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
import jakarta.persistence.EntityManager
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
    private val diaryLikeRepository: DiaryLikeRepository,
    private val diaryReactionRepository: DiaryReactionRepository,
    private val todoRepository: TodoRepository,
    private val notificationRepository: NotificationRepository,
    private val deviceRepository: DeviceRepository,
    private val uploadedImageRepository: UploadedImageRepository,
    private val childPurger: GroupRoomChildPurger,
    private val em: EntityManager
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

        // bulk JPQL delete 는 JPA cascade/orphanRemoval 을 타지 않으므로, 부모(일기·일정·유저)를
        // 지우기 전에 FK 자식부터 직접 제거한다. (기존 삭제는 그대로 두고 자식 정리만 보강)
        fun run(jpql: String, vararg params: Pair<String, Any>): Int {
            val q = em.createQuery(jpql)
            params.forEach { (k, v) -> q.setParameter(k, v) }
            return q.executeUpdate()
        }

        // 캐릭터 퀴즈/응시 정리.
        // - 내가 출제한 퀴즈는 삭제하지 않고 author 만 NULL 로 비워 보존 → 표시는 "탈퇴자".
        //   (그룹원들이 계속 풀 수 있게. character_quiz.author_id 는 nullable 로 변경됨)
        // - 내 응시기록(character_quiz_attempt.user_id 가 유저 FK)은 본인 기록이라 삭제.
        run("UPDATE CharacterQuiz q SET q.author = null WHERE q.author.id = :uid", "uid" to userId)
        run("DELETE FROM CharacterQuizAttempt a WHERE a.user.id = :uid", "uid" to userId)

        // 내가 쓴 일기에 달린 이미지/좋아요/리액션/댓글 — 일기 bulk 삭제 전에 먼저 (FK)
        run(
            "DELETE FROM DiaryImage di " +
                "WHERE di.diary.id IN (SELECT d.id FROM Diary d WHERE d.createdBy.id = :uid)",
            "uid" to userId
        )
        run(
            "DELETE FROM DiaryLike dl " +
                "WHERE dl.diary.id IN (SELECT d.id FROM Diary d WHERE d.createdBy.id = :uid)",
            "uid" to userId
        )
        run(
            "DELETE FROM DiaryReaction dr " +
                "WHERE dr.diary.id IN (SELECT d.id FROM Diary d WHERE d.createdBy.id = :uid)",
            "uid" to userId
        )
        run(
            "DELETE FROM Comment c WHERE c.targetType = :t " +
                "AND c.targetId IN (SELECT d.id FROM Diary d WHERE d.createdBy.id = :uid)",
            "t" to CommentTargetType.DIARY,
            "uid" to userId
        )
        // 내가 만든 일정에 달린 댓글 (참여자는 위에서 이미 정리됨)
        run(
            "DELETE FROM Comment c WHERE c.targetType = :t " +
                "AND c.targetId IN (SELECT s.id FROM Schedule s WHERE s.createdBy.id = :uid)",
            "t" to CommentTargetType.SCHEDULE,
            "uid" to userId
        )

        // 내가 만든 컨텐츠 삭제 (다른 사람 일기에 단 좋아요·리액션은 cascade 가 아닌 직접 정리)
        diaryLikeRepository.deleteAllByUserId(userId)
        diaryReactionRepository.deleteAllByUserId(userId)
        scheduleRepository.deleteAllByCreatedById(userId)
        commentRepository.deleteAllByCreatedById(userId)
        diaryRepository.deleteAllByCreatedById(userId)
        todoRepository.deleteAllByCreatedById(userId)

        // 알림·기기·이미지 삭제
        notificationRepository.deleteAllByUserId(userId)
        deviceRepository.deleteAllByUserId(userId)
        uploadedImageRepository.deleteAllByUserId(userId)

        em.flush()

        // 멤버십 삭제 및 빈 그룹 정리
        val groupRoomIds = memberships.map { it.groupRoom.id!! }
        memberships.forEach { membership ->
            membershipRepository.delete(membership)
        }
        membershipRepository.flush()

        // 남은 멤버가 0명인 그룹은 영구 삭제. cascade 가 닿지 못하는 자식(캐릭터·좋아요·리액션·
        // 댓글·알림)까지 GroupRoomChildPurger 로 정리한 뒤 본체 삭제(나머지는 cascade).
        groupRoomIds.forEach { groupRoomId ->
            if (membershipRepository.countByGroupRoomId(groupRoomId) == 0) {
                childPurger.purgeChildren(groupRoomId)
                groupRoomRepository.deleteById(groupRoomId)
                log.info("빈 그룹방 삭제: groupRoomId={}", groupRoomId)
            }
        }

        // 계정 삭제
        userRepository.delete(user)

        log.info("회원 탈퇴 완료: userId={}", userId)
    }
}
