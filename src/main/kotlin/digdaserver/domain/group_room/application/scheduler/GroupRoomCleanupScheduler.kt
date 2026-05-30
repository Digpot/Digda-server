package digdaserver.domain.group_room.application.scheduler

import digdaserver.domain.comment.domain.entity.CommentTargetType
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * delete_scheduled_at 이 만료된 그룹방을 영구 삭제하는 잡.
 *
 * - 매 5분마다 실행 (fixedDelay = 300_000ms).
 * - row 단위 격리: 한 그룹방 삭제가 실패해도 나머지는 계속 처리한다. 이를 위해
 *   실제 삭제 로직은 [GroupRoomPurgeExecutor.purge] 에 위임 — 별도 빈으로 호출해야
 *   Spring 의 @Transactional 프록시가 정상 동작한다 (self-invocation 회피).
 * - membership/invite/schedule(→participant)/diary(→image)/todo 는 GroupRoom 의
 *   JPA cascade(ALL)+orphanRemoval 로 정리되고, cascade 가 닿지 못하는 그룹 범위 자식
 *   (캐릭터 데이터·diary_like/reaction·comment·notification)은
 *   [GroupRoomPurgeExecutor.purgeGroupScopedChildren] 가 명시 삭제한다.
 */
@Component
class GroupRoomCleanupScheduler(
    private val groupRoomRepository: GroupRoomRepository,
    private val purgeExecutor: GroupRoomPurgeExecutor
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = FIXED_DELAY_MS, initialDelay = INITIAL_DELAY_MS)
    fun purgeScheduledGroupRooms() {
        val now = LocalDateTime.now()
        val targets = groupRoomRepository.findAllScheduledForDeletion(now)
        if (targets.isEmpty()) return

        log.info("action=그룹방 자동 정리 시작, count={}, now={}", targets.size, now)
        var success = 0
        var failed = 0
        targets.forEach { room ->
            val id = room.id
            try {
                purgeExecutor.purge(id)
                success++
            } catch (e: Exception) {
                failed++
                log.error(
                    "action=그룹방 자동 정리 실패, groupRoomId={}, error={}",
                    id,
                    e.message,
                    e
                )
            }
        }
        log.info("action=그룹방 자동 정리 완료, success={}, failed={}", success, failed)
    }

    companion object {
        private const val FIXED_DELAY_MS = 5L * 60 * 1000
        private const val INITIAL_DELAY_MS = 60L * 1000
    }
}

@Component
class GroupRoomPurgeExecutor(
    private val groupRoomRepository: GroupRoomRepository,
    private val em: EntityManager
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun purge(groupRoomId: Long) {
        val room = groupRoomRepository.findById(groupRoomId).orElse(null) ?: return
        if (room.deleteScheduledAt == null || room.deleteScheduledAt!!.isAfter(LocalDateTime.now())) {
            log.info(
                "action=그룹방 영구 삭제 건너뜀(만료 전 또는 복구됨), groupRoomId={}, deleteScheduledAt={}",
                groupRoomId,
                room.deleteScheduledAt
            )
            return
        }
        log.info(
            "action=그룹방 영구 삭제, groupRoomId={}, name={}, deleteScheduledAt={}",
            groupRoomId,
            room.name,
            room.deleteScheduledAt
        )
        // GroupRoom cascade(ALL) 가 덮지 못하는 그룹 범위 자식부터 명시 삭제한 뒤 본체 삭제.
        purgeGroupScopedChildren(groupRoomId)
        groupRoomRepository.delete(room)
    }

    /**
     * GroupRoom 의 cascade(membership·invite·schedule→participant·diary→image·todo) 가
     * 닿지 못하는 그룹 범위 자식 데이터를 FK 의존 순서로 직접 제거한다.
     *
     * - 캐릭터(모찌) 데이터는 **그룹별 소유물**이라 함께 삭제. 단 전역 마스터인
     *   `shop_item` 카탈로그는 절대 건드리지 않는다.
     * - diary_like/diary_reaction/comment 는 Diary 가 cascade 하지 않으므로, diary 가
     *   cascade 로 지워지기 전에 먼저 제거해야 FK 위반(=purge 실패)을 막는다.
     * - comment 는 polymorphic(target_type+target_id) 이라 FK 가 없어 직접 정리해야 한다.
     */
    private fun purgeGroupScopedChildren(groupRoomId: Long) {
        fun run(jpql: String, vararg params: Pair<String, Any>): Int {
            val q = em.createQuery(jpql)
            params.forEach { (k, v) -> q.setParameter(k, v) }
            return q.executeUpdate()
        }

        // 1) 캐릭터(모찌) — 그룹별 응시/퀴즈/장착/보유/본체
        val quizAttempt = run(
            "DELETE FROM CharacterQuizAttempt a " +
                "WHERE a.quiz.id IN (SELECT q.id FROM CharacterQuiz q WHERE q.groupRoom.id = :gid)",
            "gid" to groupRoomId
        )
        val quiz = run("DELETE FROM CharacterQuiz q WHERE q.groupRoom.id = :gid", "gid" to groupRoomId)
        val equipped =
            run("DELETE FROM GroupCharacterEquipped e WHERE e.groupRoom.id = :gid", "gid" to groupRoomId)
        val ownedItem =
            run("DELETE FROM GroupCharacterItem i WHERE i.groupRoom.id = :gid", "gid" to groupRoomId)
        val character =
            run("DELETE FROM GroupCharacter c WHERE c.groupRoom.id = :gid", "gid" to groupRoomId)

        // 2) 일기/일정의 비-cascade 자식 (diary 가 cascade 로 지워지기 전에 먼저)
        val diaryLike = run(
            "DELETE FROM DiaryLike dl " +
                "WHERE dl.diary.id IN (SELECT d.id FROM Diary d WHERE d.groupRoom.id = :gid)",
            "gid" to groupRoomId
        )
        val diaryReaction = run(
            "DELETE FROM DiaryReaction dr " +
                "WHERE dr.diary.id IN (SELECT d.id FROM Diary d WHERE d.groupRoom.id = :gid)",
            "gid" to groupRoomId
        )
        val diaryComment = run(
            "DELETE FROM Comment c WHERE c.targetType = :type " +
                "AND c.targetId IN (SELECT d.id FROM Diary d WHERE d.groupRoom.id = :gid)",
            "type" to CommentTargetType.DIARY,
            "gid" to groupRoomId
        )
        val scheduleComment = run(
            "DELETE FROM Comment c WHERE c.targetType = :type " +
                "AND c.targetId IN (SELECT s.id FROM Schedule s WHERE s.groupRoom.id = :gid)",
            "type" to CommentTargetType.SCHEDULE,
            "gid" to groupRoomId
        )

        // 3) 알림 (group_room_id 보유 행)
        val notification =
            run("DELETE FROM Notification n WHERE n.groupRoomId = :gid", "gid" to groupRoomId)

        // 직전 bulk delete 들이 본체 cascade 삭제보다 먼저 DB 에 반영되도록 flush.
        em.flush()
        log.info(
            "action=그룹방 자식 데이터 정리, groupRoomId={}, quizAttempt={}, quiz={}, equipped={}, " +
                "ownedItem={}, character={}, diaryLike={}, diaryReaction={}, diaryComment={}, " +
                "scheduleComment={}, notification={}",
            groupRoomId, quizAttempt, quiz, equipped, ownedItem, character,
            diaryLike, diaryReaction, diaryComment, scheduleComment, notification
        )
    }
}
