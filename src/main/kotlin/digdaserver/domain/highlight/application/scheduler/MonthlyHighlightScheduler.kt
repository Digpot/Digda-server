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

/**
 * 매월 1일 00:00(KST) 에 지난달 하이라이트를 그룹별로 생성하는 잡.
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
    fun generateOnFirstOfMonth() = runJob("cron")

    @EventListener(ApplicationReadyEvent::class)
    fun catchUpOnStartup() = runJob("startup")

    fun runJob(trigger: String) {
        val lastMonth = YearMonth.now(ZoneId.of(KST)).minusMonths(1)
        val targets = try {
            highlightRepository.findGroupIdsPendingHighlight(
                lastMonth.atDay(1),
                lastMonth.atEndOfMonth(),
                lastMonth.toString()
            )
        } catch (e: Exception) {
            log.error("action=월간 하이라이트 대상 조회 실패, trigger={}, month={}", trigger, lastMonth, e)
            return
        }
        if (targets.isEmpty()) return

        log.info("action=월간 하이라이트 생성 시작, trigger={}, month={}, groups={}", trigger, lastMonth, targets.size)
        var created = 0
        var failed = 0
        targets.forEach { groupId ->
            try {
                if (generator.generate(groupId, lastMonth)) {
                    created++
                    notifySafely(groupId, lastMonth)
                }
            } catch (e: DataIntegrityViolationException) {
                // 다른 인스턴스/동시 실행이 먼저 만들었다 — 정상 경합.
                log.info("action=월간 하이라이트 이미 생성됨(경합), groupId={}, month={}", groupId, lastMonth)
            } catch (e: Exception) {
                failed++
                log.error("action=월간 하이라이트 생성 실패, groupId={}, month={}", groupId, lastMonth, e)
            }
        }
        log.info("action=월간 하이라이트 생성 완료, month={}, created={}, failed={}", lastMonth, created, failed)
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
