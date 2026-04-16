package digdaserver.domain.diary.presentation.controller

import digdaserver.domain.diary.application.service.DiaryService
import digdaserver.domain.diary.presentation.dto.req.CreateDiaryRequest
import digdaserver.domain.diary.presentation.dto.req.UpdateDiaryRequest
import digdaserver.domain.diary.presentation.dto.res.DiaryCalendarResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryDetailResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryListResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
import java.time.YearMonth
import java.util.UUID

@RestController
@Tag(name = "Diary", description = "일기 API")
class DiaryController(
    private val diaryService: DiaryService
) {

    @Operation(summary = "일기 목록 조회", description = "그룹방의 일기 목록을 조회합니다. 월별 필터 및 페이지네이션을 지원합니다.")
    @GetMapping("/group-rooms/{groupRoomId}/diaries")
    fun getDiaries(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @RequestParam(required = false) month: YearMonth?,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<DiaryListResponse> {
        val response = diaryService.getDiaries(UUID.fromString(userId), groupRoomId, month, limit, offset)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "일기 캘린더 조회", description = "해당 월에 일기가 존재하는 날짜 배열을 반환합니다.")
    @GetMapping("/group-rooms/{groupRoomId}/diaries/calendar")
    fun getDiaryCalendar(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @RequestParam month: YearMonth
    ): ResponseEntity<DiaryCalendarResponse> {
        val response = diaryService.getDiaryCalendar(UUID.fromString(userId), groupRoomId, month)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "일기 상세 조회", description = "일기 상세 정보와 댓글 목록을 조회합니다.")
    @GetMapping("/group-rooms/{groupRoomId}/diaries/{diaryId}")
    fun getDiaryDetail(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable diaryId: Long
    ): ResponseEntity<DiaryDetailResponse> {
        val response = diaryService.getDiaryDetail(UUID.fromString(userId), groupRoomId, diaryId)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "일기 작성", description = "새 일기를 작성합니다.")
    @PostMapping("/group-rooms/{groupRoomId}/diaries")
    fun createDiary(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @RequestBody request: CreateDiaryRequest
    ): ResponseEntity<DiaryResponse> {
        val response = diaryService.createDiary(UUID.fromString(userId), groupRoomId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "일기 수정", description = "일기를 수정합니다. 작성자 또는 방장만 가능합니다.")
    @PutMapping("/group-rooms/{groupRoomId}/diaries/{diaryId}")
    fun updateDiary(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable diaryId: Long,
        @RequestBody request: UpdateDiaryRequest
    ): ResponseEntity<DiaryResponse> {
        val response = diaryService.updateDiary(UUID.fromString(userId), groupRoomId, diaryId, request)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "일기 삭제", description = "일기를 삭제합니다. 작성자 또는 방장만 가능합니다.")
    @DeleteMapping("/group-rooms/{groupRoomId}/diaries/{diaryId}")
    fun deleteDiary(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable diaryId: Long
    ): ResponseEntity<Void> {
        diaryService.deleteDiary(UUID.fromString(userId), groupRoomId, diaryId)
        return ResponseEntity.noContent().build()
    }
}
