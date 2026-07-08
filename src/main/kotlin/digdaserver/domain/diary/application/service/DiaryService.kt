package digdaserver.domain.diary.application.service

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
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

interface DiaryService {

    fun getDiaries(userId: UUID, groupRoomId: Long, month: YearMonth?, limit: Int, offset: Int): DiaryListResponse

    /**
     * 시그니처 지도 — 그룹의 지역별 일기 수 집계.
     * [scope] 가 "claim" 이면 정복 칭호 판정용으로 **사용자 가입 이후 작성분만** 집계하고
     * 어드민 채움은 제외한다(소급 칭호 방지). 그 외엔 그룹 전체(표시용).
     */
    fun getDiaryRegionMap(userId: UUID, groupRoomId: Long, scope: String? = null): DiaryRegionMapResponse

    /** 시그니처 지도 — 특정 지역(regionKey)의 그룹 일기 목록. */
    fun getDiariesByRegion(
        userId: UUID,
        groupRoomId: Long,
        regionKey: String,
        limit: Int,
        offset: Int
    ): DiaryListResponse

    fun getDiaryCalendar(userId: UUID, groupRoomId: Long, month: YearMonth): DiaryCalendarResponse

    /** 특정 날짜의 그룹 일기 전부 — 캘린더 날짜 탭 → 일기 목록 화면용. */
    fun getDiariesByDate(userId: UUID, groupRoomId: Long, date: LocalDate): DiaryDayResponse

    /** 대표 썸네일 지정 — 그룹원 누구나 가능. 같은 날의 기존 대표는 해제된다. */
    fun setRepresentative(userId: UUID, groupRoomId: Long, diaryId: Long): DiaryDayResponse

    fun getDiaryDetail(userId: UUID, groupRoomId: Long, diaryId: Long): DiaryDetailResponse

    fun createDiary(userId: UUID, groupRoomId: Long, request: CreateDiaryRequest): DiaryResponse

    fun updateDiary(userId: UUID, groupRoomId: Long, diaryId: Long, request: UpdateDiaryRequest): DiaryResponse

    fun deleteDiary(userId: UUID, groupRoomId: Long, diaryId: Long)

    fun toggleLike(userId: UUID, groupRoomId: Long, diaryId: Long): DiaryLikeResponse

    fun toggleReaction(
        userId: UUID,
        groupRoomId: Long,
        diaryId: Long,
        request: ToggleDiaryReactionRequest
    ): DiaryReactionToggleResponse
}
