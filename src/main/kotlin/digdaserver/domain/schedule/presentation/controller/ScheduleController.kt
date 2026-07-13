package digdaserver.domain.schedule.presentation.controller

import digdaserver.domain.schedule.application.service.ScheduleService
import digdaserver.domain.schedule.presentation.dto.req.CopyScheduleRequest
import digdaserver.domain.schedule.presentation.dto.req.CreateScheduleRequest
import digdaserver.domain.schedule.presentation.dto.req.UpdateScheduleRequest
import digdaserver.domain.schedule.presentation.dto.res.ScheduleDetailResponse
import digdaserver.domain.schedule.presentation.dto.res.ScheduleListResponse
import digdaserver.domain.schedule.presentation.dto.res.ScheduleResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@Tag(name = "Schedule", description = "일정 API")
class ScheduleController(
    private val scheduleService: ScheduleService
) {

    @Operation(summary = "일정 목록 조회 (기간)", description = "시작일~종료일 범위의 일정 목록을 조회합니다.")
    @GetMapping("/group-rooms/{groupRoomId}/schedules")
    fun getSchedules(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate
    ): ResponseEntity<ScheduleListResponse> {
        val response = scheduleService.getSchedules(UUID.fromString(userId), groupRoomId, startDate, endDate)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "일정 상세 조회", description = "일정 상세 정보와 댓글 목록을 조회합니다.")
    @GetMapping("/group-rooms/{groupRoomId}/schedules/{scheduleId}")
    fun getScheduleDetail(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable scheduleId: Long
    ): ResponseEntity<ScheduleDetailResponse> {
        val response = scheduleService.getScheduleDetail(UUID.fromString(userId), groupRoomId, scheduleId)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "일정 생성", description = "새 일정을 생성합니다. 참여자를 지정할 수 있습니다.")
    @PostMapping("/group-rooms/{groupRoomId}/schedules")
    fun createSchedule(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @RequestBody request: CreateScheduleRequest
    ): ResponseEntity<ScheduleResponse> {
        val response = scheduleService.createSchedule(UUID.fromString(userId), groupRoomId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(
        summary = "일정 여러 날짜 복사",
        description = "일정을 선택한 날짜들에 복사합니다. 기간 일정은 길이를 유지하고, 참여자도 함께 복사됩니다. 한 번에 최대 31개 날짜."
    )
    @PostMapping("/group-rooms/{groupRoomId}/schedules/{scheduleId}/copy")
    fun copySchedule(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable scheduleId: Long,
        @RequestBody request: CopyScheduleRequest
    ): ResponseEntity<ScheduleListResponse> {
        val response = scheduleService.copySchedule(UUID.fromString(userId), groupRoomId, scheduleId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "일정 수정", description = "일정을 수정합니다. 작성자 또는 방장만 가능합니다.")
    @PutMapping("/group-rooms/{groupRoomId}/schedules/{scheduleId}")
    fun updateSchedule(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable scheduleId: Long,
        @RequestBody request: UpdateScheduleRequest
    ): ResponseEntity<ScheduleResponse> {
        val response = scheduleService.updateSchedule(UUID.fromString(userId), groupRoomId, scheduleId, request)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "일정 삭제", description = "일정을 삭제합니다. 작성자 또는 방장만 가능합니다.")
    @DeleteMapping("/group-rooms/{groupRoomId}/schedules/{scheduleId}")
    fun deleteSchedule(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable scheduleId: Long
    ): ResponseEntity<Void> {
        scheduleService.deleteSchedule(UUID.fromString(userId), groupRoomId, scheduleId)
        return ResponseEntity.noContent().build()
    }
}
