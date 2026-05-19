package digdaserver.domain.group_room.application.scheduler

import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
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
 * - JPA cascade(ALL) + orphanRemoval 로 membership/invite/schedule/diary/todo 가
 *   함께 정리된다.
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
    private val groupRoomRepository: GroupRoomRepository
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
        groupRoomRepository.delete(room)
    }
}
