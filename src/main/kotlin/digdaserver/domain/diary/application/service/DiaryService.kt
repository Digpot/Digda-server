package digdaserver.domain.diary.application.service

import digdaserver.domain.diary.presentation.dto.req.CreateDiaryRequest
import digdaserver.domain.diary.presentation.dto.req.ToggleDiaryReactionRequest
import digdaserver.domain.diary.presentation.dto.req.UpdateDiaryRequest
import digdaserver.domain.diary.presentation.dto.res.DiaryCalendarResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryDetailResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryLikeResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryListResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryReactionToggleResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryResponse
import java.time.YearMonth
import java.util.UUID

interface DiaryService {

    fun getDiaries(userId: UUID, groupRoomId: Long, month: YearMonth?, limit: Int, offset: Int): DiaryListResponse

    fun getDiaryCalendar(userId: UUID, groupRoomId: Long, month: YearMonth): DiaryCalendarResponse

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
