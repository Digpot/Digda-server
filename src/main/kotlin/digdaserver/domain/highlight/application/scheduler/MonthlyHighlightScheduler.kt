package digdaserver.domain.highlight.application.scheduler

import digdaserver.domain.highlight.application.service.MonthlyHighlightGenerator
import digdaserver.domain.highlight.domain.repository.MonthlyHighlightRepository
import digdaserver.domain.notification.application.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.YearMonth
import java.time.ZoneId

/** 한 번의 하이라이트 생성 실행 결과. */
data class HighlightRunResult(
    val month: String,
    val targetGroups: Int,
    val created: Int,
    val skipped: Int,
    val failed: Int
)

/**
 * 매월 1일 00:00(KST) 에 지난달 하이라이트를 그룹별로 생성하는 잡. 어드민 수동 실행도 [execute] 를 공유한다.
 *
 * - 대상: 지난달에 일기가 있고 아직 하이라이트가 없는 그룹만. 일기가 없는 그룹은 만들지 않는다.
 * - 중복 방지: ① 대상 쿼리가 이미 있는 그룹을 제외 ② 생성기가 한 번 더 확인 ③ DB 유니크 제약.
 * - 재기동/일시 오류 복구: 부팅 직후에도 같은 잡을 돌려(catch-up) 자정에 서버가 꺼져 있었거나
 *   일부 그룹이 실패했어도 다음 기회에 이어서 채운다. 멱등이라 여러 번 돌려도 안전하다.
 * - row 단위 격리: 한 그룹 실패가 나머지를 막지 않는다. 알림은 커밋 이후 별도로 보내고 실패해도 무시한다.
 */
@Component
class MonthlyHighlightScheduler(
    private val highlightRepository: MonthlyHighlightRepository,
    private val generator: MonthlyHighlightGenerator,
    private val notificationService: NotificationService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 0 1 * *", zone = KST)
    fun generateOnFirstOfMonth() {
        execute(lastMonth(), groupId = null, regenerate = false, notify = true, trigger = "cron")
    }

    @EventListener(ApplicationReadyEvent::class)
    fun catchUpOnStartup() {
        execute(lastMonth(), groupId = null, regenerate = false, notify = true, trigger = "startup")
    }

    fun lastMonth(): YearMonth = YearMonth.now(ZoneId.of(KST)).minusMonths(1)

    /**
     * @param groupId 지정하면 그 그룹만, null 이면 전체.
     * @param regenerate true 면 이미 있는 하이라이트도 지우고 다시 만든다(알림은 보내지 않는다).
     * @param notify 새로 만든 그룹에 알림을 보낼지.
     */
    fun execute(
        month: YearMonth,
        groupId: Long?,
        regenerate: Boolean,
        notify: Boolean,
        trigger: String
    ): HighlightRunResult {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        val targets = try {
            when {
                groupId != null -> listOf(groupId)
                regenerate -> highlightRepository.findGroupIdsWithDiaries(start, end)
                else -> highlightRepository.findGroupIdsPendingHighlight(start, end, month.toString())
            }
        } catch (e: Exception) {
            log.error("action=월간 하이라이트 대상 조회 실패, trigger={}, month={}", trigger, month, e)
            return HighlightRunResult(month.toString(), 0, 0, 0, 1)
        }
        if (targets.isEmpty()) return HighlightRunResult(month.toString(), 0, 0, 0, 0)

        log.info("action=월간 하이라이트 생성 시작, trigger={}, month={}, groups={}", trigger, month, targets.size)
        var created = 0
        var skipped = 0
        var failed = 0
        targets.forEach { id ->
            try {
                val made = if (regenerate) generator.regenerate(id, month) else generator.generate(id, month)
                if (made) {
                    created++
                    if (notify && !regenerate) notifySafely(id, month)
                } else {
                    skipped++
                }
            } catch (e: DataIntegrityViolationException) {
                // 다른 인스턴스/동시 실행이 먼저 만들었다 — 정상 경합.
                skipped++
                log.info("action=월간 하이라이트 이미 생성됨(경합), groupId={}, month={}", id, month)
            } catch (e: Exception) {
                failed++
                log.error("action=월간 하이라이트 생성 실패, groupId={}, month={}", id, month, e)
            }
        }
        log.info(
            "action=월간 하이라이트 생성 완료, trigger={}, month={}, created={}, skipped={}, failed={}",
            trigger,
            month,
            created,
            skipped,
            failed
        )
        return HighlightRunResult(month.toString(), targets.size, created, skipped, failed)
    }

    /**
     * 이미 만들어진 하이라이트의 알림만 다시 보낸다(어드민). 새로 생성하지 않는다.
     * 삭제됐거나 하이라이트가 없는 그룹은 건너뛴다.
     */
    fun resendNotifications(month: YearMonth, groupId: Long?): HighlightRunResult {
        val existing = highlightRepository.findGroupIdsByTargetMonth(month.toString())
        val targets = if (groupId != null) existing.filter { it == groupId } else existing
        var sent = 0
        var failed = 0
        targets.forEach {
            try {
                notificationService.notifyMonthlyHighlight(it, month.toString())
                sent++
            } catch (e: Exception) {
                failed++
                log.error("action=월간 하이라이트 알림 재발송 실패, groupId={}, month={}", it, month, e)
            }
        }
        log.info("action=월간 하이라이트 알림 재발송, month={}, sent={}, failed={}", month, sent, failed)
        return HighlightRunResult(month.toString(), targets.size, sent, 0, failed)
    }

    private fun notifySafely(groupId: Long, month: YearMonth) {
        try {
            notificationService.notifyMonthlyHighlight(groupId, month.toString())
        } catch (e: Exception) {
            log.warn("action=월간 하이라이트 알림 실패, groupId={}, month={}, error={}", groupId, month, e.message)
        }
    }

    companion object {
        private const val KST = "Asia/Seoul"
    }
}
