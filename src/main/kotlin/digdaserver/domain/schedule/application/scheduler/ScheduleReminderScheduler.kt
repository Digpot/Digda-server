package digdaserver.domain.schedule.application.scheduler

import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.schedule.domain.entity.Schedule
import digdaserver.domain.schedule.domain.repository.ScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * 일정 시작일을 기준으로 리마인더 알림을 발송하는 잡.
 *
 * - 매일 오전 9시(KST)에 1회 실행.
 * - 시작일이 "내일"인 일정 → 하루 전 리마인더(SCHEDULE_DAY_BEFORE).
 * - 시작일이 "오늘"인 일정 → 당일 리마인더(SCHEDULE_TODAY).
 * - 발송 대상: 일정 참가자 전원 + 일정을 생성한 사람(중복은 제거).
 * - 중복 발송 방지는 [NotificationService] 내부에서 처리한다. 동일 일정·동일 종류의
 *   리마인더가 이미 존재하면 건너뛰므로, 잡이 재실행되어도 같은 알림이 두 번 가지 않는다.
 * - row 단위 격리: 한 일정의 처리가 실패해도 나머지 일정은 계속 처리한다.
 */
@Component
class ScheduleReminderScheduler(
    private val scheduleRepository: ScheduleRepository,
    private val notificationService: NotificationService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = CRON_DAILY_9AM, zone = KST)
    fun sendScheduleReminders() {
        val today = LocalDate.now(ZoneId.of(KST))

        dispatch(targetDate = today.plusDays(1), isToday = false)
        dispatch(targetDate = today, isToday = true)
    }

    private fun dispatch(targetDate: LocalDate, isToday: Boolean) {
        val label = if (isToday) "당일" else "하루 전"
        val schedules = scheduleRepository.findAllForReminder(targetDate)
        if (schedules.isEmpty()) return

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
        // 초 분 시 일 월 요일 — 매일 09:00 (KST)
        private const val CRON_DAILY_9AM = "0 0 9 * * *"
        private const val KST = "Asia/Seoul"
    }
}
