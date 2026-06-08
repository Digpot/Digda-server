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
 * - KST 09:00 / 12:00 / 18:00 세 슬롯에서만 실행. 9시 이후 등록된 일정이라도
 *   같은 날 안에 다음 슬롯이 따라잡아 발송한다. 중복 방지는 [NotificationService] 가 담당.
 * - 시작일이 "내일"인 일정 → 하루 전 리마인더(SCHEDULE_DAY_BEFORE), 시작 하루 전 1회.
 * - "오늘" 진행 중인(시작일 ≤ 오늘 ≤ 종료일) 일정 → 당일 리마인더(SCHEDULE_TODAY).
 *   멀티데이 일정은 중간 날에도 당일 알림이 간다(예: 6~8일 일정이면 6·7·8일 모두).
 * - 발송 대상: 일정 참가자 전원 + 일정을 생성한 사람(중복은 제거).
 * - 중복 발송 방지는 [NotificationService] 가 시간창(12h) 멱등으로 처리한다. 같은 날
 *   여러 슬롯의 중복은 막고, 멀티데이의 다음 날 당일 알림은 통과시킨다.
 * - row 단위 격리: 한 일정의 처리가 실패해도 나머지 일정은 계속 처리한다.
 */
@Component
class ScheduleReminderScheduler(
    private val scheduleRepository: ScheduleRepository,
    private val notificationService: NotificationService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = CRON_3_SLOTS_DAILY, zone = KST)
    fun sendScheduleReminders() {
        runReminderJob(trigger = "cron")
    }

    /**
     * 부팅 직후 catch-up. cron 슬롯은 09/12/18시 정각에만 발사되므로
     * 그 사이에 재배포/재시작이 끼면 그날치 리마인더가 유실될 수 있다.
     * 첫 슬롯(09:00) 이전엔 너무 이른 발송이라 건너뛰고, 그 외엔 catch-up 실행.
     * 시간창(12h) 멱등 가드가 있어 cron 이 이미 보냈으면 중복 발송되지 않는다.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun catchUpOnStartup() {
        val nowKst = LocalTime.now(ZoneId.of(KST))
        val firstSlot = LocalTime.of(FIRST_SLOT_HOUR, 0)
        if (nowKst.isBefore(firstSlot)) {
            log.info(
                "action=일정 리마인더 startup catch-up 건너뜀(첫 슬롯 이전), nowKst={}",
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
        // 당일 리마인더는 그날 '진행 중'인 일정 전체(멀티데이 중간 날 포함)를 대상으로 한다.
        // 하루 전 리마인더는 '시작일이 내일'인 일정만(시작 하루 전 1회).
        val schedules = if (isToday) {
            scheduleRepository.findAllActiveOnDate(targetDate)
        } else {
            scheduleRepository.findAllForReminder(targetDate)
        }
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
        private const val FIRST_SLOT_HOUR = 9

        // 초 분 시 일 월 요일 — KST 09:00 / 12:00 / 18:00 세 슬롯 (재실행은 멱등).
        private const val CRON_3_SLOTS_DAILY = "0 0 9,12,18 * * *"
        private const val KST = "Asia/Seoul"
    }
}
