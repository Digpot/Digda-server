package digdaserver.domain.diary.presentation.controller

import digdaserver.domain.diary.application.service.DiaryService
import digdaserver.domain.diary.presentation.dto.req.CreateDiaryRequest
import digdaserver.domain.diary.presentation.dto.req.ToggleDiaryReactionRequest
import digdaserver.domain.diary.presentation.dto.req.UpdateDiaryRequest
import digdaserver.domain.diary.presentation.dto.res.DiaryCalendarResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryDayResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryDetailResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryLikeResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryListResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryReactionToggleResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryRegionMapResponse
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
import java.time.LocalDate
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

    @Operation(
        summary = "일기 지역지도 집계",
        description = "시그니처 지도용. 그룹의 region_key 별 일기 수를 집계해 반환합니다. 색칠 임계값 판정은 앱이 수행."
    )
    @GetMapping("/group-rooms/{groupRoomId}/diaries/region-map")
    fun getDiaryRegionMap(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @RequestParam(required = false) scope: String?
    ): ResponseEntity<DiaryRegionMapResponse> {
        val response = diaryService.getDiaryRegionMap(UUID.fromString(userId), groupRoomId, scope)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "지역별 일기 목록", description = "시그니처 지도에서 특정 지역(regionKey)을 선택했을 때의 일기 목록.")
    @GetMapping("/group-rooms/{groupRoomId}/diaries/by-region")
    fun getDiariesByRegion(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @RequestParam regionKey: String,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<DiaryListResponse> {
        val response = diaryService.getDiariesByRegion(UUID.fromString(userId), groupRoomId, regionKey, limit, offset)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "날짜별 일기 목록", description = "특정 날짜의 그룹 일기 전부(먼저 쓴 순)와 대표 썸네일/내 일기 id 를 반환합니다.")
    @GetMapping("/group-rooms/{groupRoomId}/diaries/by-date")
    fun getDiariesByDate(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @RequestParam date: LocalDate
    ): ResponseEntity<DiaryDayResponse> {
        val response = diaryService.getDiariesByDate(UUID.fromString(userId), groupRoomId, date)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "대표 썸네일 지정", description = "해당 일기를 그날의 대표 썸네일로 지정합니다. 그룹원 누구나 가능하며 기존 대표는 해제됩니다.")
    @PutMapping("/group-rooms/{groupRoomId}/diaries/{diaryId}/representative")
    fun setRepresentative(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable diaryId: Long
    ): ResponseEntity<DiaryDayResponse> {
        val response = diaryService.setRepresentative(UUID.fromString(userId), groupRoomId, diaryId)
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

    @Operation(summary = "일기 작성", description = "새 일기를 작성합니다. 이미지는 imageIds 배열로 0..10장.")
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

    @Operation(summary = "일기 좋아요 토글", description = "해당 일기에 좋아요를 누르거나 취소합니다.")
    @PostMapping("/group-rooms/{groupRoomId}/diaries/{diaryId}/like")
    fun toggleLike(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable diaryId: Long
    ): ResponseEntity<DiaryLikeResponse> {
        val response = diaryService.toggleLike(UUID.fromString(userId), groupRoomId, diaryId)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "일기 이모지 리액션 토글", description = "type 이미 누른 상태면 취소, 아니면 추가.")
    @PostMapping("/group-rooms/{groupRoomId}/diaries/{diaryId}/reactions")
    fun toggleReaction(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable diaryId: Long,
        @RequestBody request: ToggleDiaryReactionRequest
    ): ResponseEntity<DiaryReactionToggleResponse> {
        val response = diaryService.toggleReaction(UUID.fromString(userId), groupRoomId, diaryId, request)
        return ResponseEntity.ok(response)
    }
}
