package digdaserver.domain.schedule.application.scheduler

import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.schedule.domain.entity.Schedule
import digdaserver.domain.schedule.domain.repository.ScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * 일정 시작일을 기준으로 리마인더 알림을 발송하는 잡.
 *
 * - 매시 정각(KST 09:00 ~ 23:00) 실행. 첫 발송은 09:00이지만,
 *   배포·재시작·9시 이후 새로 생긴 일정 등으로 9시 슬롯이 비더라도
 *   같은 날 안에 자동으로 따라잡는다. 중복 방지는 [NotificationService] 가 담당.
 * - 시작일이 "내일"인 일정 → 하루 전 리마인더(SCHEDULE_DAY_BEFORE).
 * - 시작일이 "오늘"인 일정 → 당일 리마인더(SCHEDULE_TODAY).
 * - 발송 대상: 일정 참가자 전원 + 일정을 생성한 사람(중복은 제거).
 * - 중복 발송 방지는 [NotificationService] 내부에서 처리한다. 동일 일정·동일 종류의
 *   리마인더가 이미 존재하면 건너뛰므로, 잡이 시간당 다시 돌아도 같은 알림이 두 번 가지 않는다.
 * - row 단위 격리: 한 일정의 처리가 실패해도 나머지 일정은 계속 처리한다.
 */
@Component
class ScheduleReminderScheduler(
    private val scheduleRepository: ScheduleRepository,
    private val notificationService: NotificationService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = CRON_HOURLY_9_TO_23, zone = KST)
    fun sendScheduleReminders() {
        runReminderJob(trigger = "cron")
    }

    /**
     * 부팅 직후 catch-up. cron 슬롯은 [REMINDER_START_HOUR]~[REMINDER_END_HOUR] 매시 정각에만
     * 발사되므로, 그 사이에 재배포/재시작이 끼면 그날치 리마인더가 통째로 유실될 수 있다.
     * 멱등 가드(`existsByTypeAndRelatedId`)가 있어 cron 이 이미 보냈으면 중복 발송되지 않는다.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun catchUpOnStartup() {
        val nowKst = LocalTime.now(ZoneId.of(KST))
        val startBoundary = LocalTime.of(REMINDER_START_HOUR, 0)
        val endBoundary = LocalTime.of(REMINDER_END_HOUR, 59)
        if (nowKst.isBefore(startBoundary) || nowKst.isAfter(endBoundary)) {
            log.info(
                "action=일정 리마인더 startup catch-up 건너뜀(시간대 외), nowKst={}",
                nowKst
            )
            return
        }
        log.info("action=일정 리마인더 startup catch-up 실행, nowKst={}", nowKst)
        runReminderJob(trigger = "startup")
    }

    private fun runReminderJob(trigger: String) {
        val today = LocalDate.now(ZoneId.of(KST))
        log.info("action=일정 리마인더 진입, trigger={}, today(KST)={}", trigger, today)

        dispatch(targetDate = today.plusDays(1), isToday = false)
        dispatch(targetDate = today, isToday = true)
    }

    private fun dispatch(targetDate: LocalDate, isToday: Boolean) {
        val label = if (isToday) "당일" else "하루 전"
        val schedules = scheduleRepository.findAllForReminder(targetDate)
        if (schedules.isEmpty()) {
            // 0건도 항상 로그 — 스케줄러가 살아 있는지 / 쿼리 결과가 비는 건지 구분 가능하게.
            log.info(
                "action=일정 리마인더 대상 없음, type={}, targetDate={}",
                label,
                targetDate
            )
            return
        }

        log.info(
            "action=일정 리마인더 발송 시작, type={}, targetDate={}, count={}",
            label,
            targetDate,
            schedules.size
        )

        var success = 0
        var failed = 0
        schedules.forEach { schedule ->
            try {
                val recipientUserIds = resolveRecipients(schedule)
                if (isToday) {
                    notificationService.notifyScheduleReminderToday(
                        groupRoomId = schedule.groupRoom.id,
                        scheduleId = schedule.id,
                        scheduleTitle = schedule.title,
                        recipientUserIds = recipientUserIds
                    )
                } else {
                    notificationService.notifyScheduleReminderDayBefore(
                        groupRoomId = schedule.groupRoom.id,
                        scheduleId = schedule.id,
                        scheduleTitle = schedule.title,
                        recipientUserIds = recipientUserIds
                    )
                }
                success++
            } catch (e: Exception) {
                failed++
                log.error(
                    "action=일정 리마인더 발송 실패, type={}, scheduleId={}, error={}",
                    label,
                    schedule.id,
                    e.message,
                    e
                )
            }
        }

        log.info(
            "action=일정 리마인더 발송 완료, type={}, success={}, failed={}",
            label,
            success,
            failed
        )
    }

    /**
     * 리마인더 수신 대상 = 일정 참가자 전원 + 일정 생성자.
     * createdBy / participant.user 는 LAZY 이지만 id 접근만 하므로 추가 쿼리가 발생하지 않는다.
     */
    private fun resolveRecipients(schedule: Schedule): List<UUID> =
        (schedule.participants.map { it.user.id } + schedule.createdBy.id).distinct()

    companion object {
        private const val REMINDER_START_HOUR = 9
        private const val REMINDER_END_HOUR = 23

        // 초 분 시 일 월 요일 — KST 09:00 ~ 23:00 매시 정각 (재실행은 멱등).
        private const val CRON_HOURLY_9_TO_23 = "0 0 9-23 * * *"
        private const val KST = "Asia/Seoul"
    }
}
