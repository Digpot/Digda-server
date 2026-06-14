package digdaserver.domain.diary.application.service.impl

import digdaserver.domain.block.application.service.ContentVisibilityService
import digdaserver.domain.block.application.service.VisibilityReason
import digdaserver.domain.character.application.service.CharacterService
import digdaserver.domain.comment.domain.entity.CommentTargetType
import digdaserver.domain.comment.domain.repository.CommentRepository
import digdaserver.domain.diary.application.service.DiaryService
import digdaserver.domain.diary.domain.entity.Diary
import digdaserver.domain.diary.domain.entity.DiaryLike
import digdaserver.domain.diary.domain.entity.DiaryReaction
import digdaserver.domain.diary.domain.entity.DiaryReactionType
import digdaserver.domain.diary.domain.repository.DiaryLikeRepository
import digdaserver.domain.diary.domain.repository.DiaryReactionRepository
import digdaserver.domain.diary.domain.repository.DiaryRepository
import digdaserver.domain.diary.domain.repository.GroupRegionFillRepository
import digdaserver.domain.diary.presentation.dto.req.CreateDiaryRequest
import digdaserver.domain.diary.presentation.dto.req.ToggleDiaryReactionRequest
import digdaserver.domain.diary.presentation.dto.req.UpdateDiaryRequest
import digdaserver.domain.diary.presentation.dto.res.DiaryCalendarEntry
import digdaserver.domain.diary.presentation.dto.res.DiaryCalendarResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryCalendarStats
import digdaserver.domain.diary.presentation.dto.res.DiaryCommentResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryDetailResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryLikeResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryListResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryReactionSummary
import digdaserver.domain.diary.presentation.dto.res.DiaryReactionToggleResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryRegionCount
import digdaserver.domain.diary.presentation.dto.res.DiaryRegionMapResponse
import digdaserver.domain.diary.presentation.dto.res.DiaryResponse
import digdaserver.domain.diary.presentation.dto.res.DiarySummaryResponse
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.log.application.service.UserActionLogService
import digdaserver.domain.log.domain.entity.UserAction
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.upload.domain.repository.UploadedImageRepository
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

@Service
@Transactional(readOnly = true)
class DiaryServiceImpl(
    private val diaryRepository: DiaryRepository,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val userActionLogService: UserActionLogService,
    private val uploadedImageRepository: UploadedImageRepository,
    private val diaryLikeRepository: DiaryLikeRepository,
    private val diaryReactionRepository: DiaryReactionRepository,
    private val groupRegionFillRepository: GroupRegionFillRepository,
    private val contentVisibilityService: ContentVisibilityService,
    @Lazy private val characterService: CharacterService
) : DiaryService {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_IMAGES_PER_DIARY = 10

        /** 어드민이 임의로 채운 지역의 합성 일기 수 — 최대 색칠 임계(광역시 10) 이상이면 무조건 색칠된다. */
        private const val ADMIN_FILL_COUNT = 10L

        /**
         * 날짜 판정 기준 타임존. 서버 기본 TZ 가 UTC 이면 KST 00~09 시 사이에 한국 사용자의
         * "오늘" 이 서버의 UTC "오늘" 보다 하루 앞서 미래로 오판될 수 있어, 한국 기준으로 고정한다.
         */
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")

        /** 일기 1건 작성 시 모찌에게 지급하는 경험치. */
        private const val DIARY_WRITE_EXP = 10

        /** 연속 기록(streak) 계산 시 거슬러 올라가 조회할 최근 일 수. */
        private const val STREAK_WINDOW_DAYS = 400L

        /** 일기 작성·수정·삭제가 가능한 기간(개월). 앱(diary_window.dart)과 같은 값으로 맞춘다. */
        private const val EDITABLE_MONTHS = 3L
    }

    /** 날짜 미래 여부 판정에 쓰는 "오늘"(한국 기준). */
    private fun todayKst(): LocalDate = LocalDate.now(KST)

    /** 편집 가능한 가장 이른 날짜 — 오늘(KST)에서 [EDITABLE_MONTHS] 개월 전. */
    private fun editableFrom(): LocalDate = todayKst().minusMonths(EDITABLE_MONTHS)

    override fun getDiaries(
        userId: UUID,
        groupRoomId: Long,
        month: YearMonth?,
        limit: Int,
        offset: Int
    ): DiaryListResponse {
        ensureMember(userId, groupRoomId)

        // limit=0 같은 값이 와도 0-나눗셈/IllegalArgument 로 500 나지 않도록 하한 보정.
        val safeLimit = limit.coerceAtLeast(1)
        val pageable = PageRequest.of(offset / safeLimit, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"))

        val page = if (month != null) {
            val startDate = month.atDay(1)
            val endDate = month.atEndOfMonth()
            diaryRepository.findAllByGroupRoomIdAndDateBetween(groupRoomId, startDate, endDate, pageable)
        } else {
            diaryRepository.findAllByGroupRoomId(groupRoomId, pageable)
        }

        return buildListResponse(page, userId)
    }

    override fun getDiaryRegionMap(userId: UUID, groupRoomId: Long, scope: String?): DiaryRegionMapResponse {
        ensureMember(userId, groupRoomId)

        // 정복 칭호 판정용(claim): 가입 이후 작성분만 + 어드민 채움 제외 → 소급 칭호 차단.
        if (scope == "claim") {
            val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
                .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }
            val rows = diaryRepository.countByRegionKeySince(groupRoomId, membership.joinedAt)
            val counts = LinkedHashMap<String, Long>()
            for (row in rows) counts[row[0] as String] = row[1] as Long
            val regions = counts.map { DiaryRegionCount(regionKey = it.key, count = it.value) }
            log.info(
                "action=일기 지역지도 집계(claim), groupRoomId={}, userId={}, since={}, regions={}",
                groupRoomId,
                userId,
                membership.joinedAt,
                regions.size
            )
            return DiaryRegionMapResponse(regions = regions, total = regions.sumOf { it.count }, adminFilledKeys = emptyList())
        }

        val rows = diaryRepository.countByRegionKey(groupRoomId)
        val counts = LinkedHashMap<String, Long>()
        for (row in rows) {
            counts[row[0] as String] = row[1] as Long
        }
        // 어드민이 임의로 채운 지역을 병합 — 색칠 임계(최대 10) 이상으로 끌어올린다.
        val filled = groupRegionFillRepository.findAllByGroupRoomId(groupRoomId)
        for (f in filled) {
            val cur = counts[f.regionKey] ?: 0L
            if (cur < ADMIN_FILL_COUNT) counts[f.regionKey] = ADMIN_FILL_COUNT
        }
        val regions = counts.map { DiaryRegionCount(regionKey = it.key, count = it.value) }
        val total = regions.sumOf { it.count }
        log.info("action=일기 지역지도 집계, groupRoomId={}, regions={}, total={}", groupRoomId, regions.size, total)
        return DiaryRegionMapResponse(
            regions = regions,
            total = total,
            adminFilledKeys = filled.map { it.regionKey }
        )
    }

    override fun getDiariesByRegion(
        userId: UUID,
        groupRoomId: Long,
        regionKey: String,
        limit: Int,
        offset: Int
    ): DiaryListResponse {
        ensureMember(userId, groupRoomId)
        val safeLimit = limit.coerceAtLeast(1)
        val pageable = PageRequest.of(offset / safeLimit, safeLimit)
        val page = diaryRepository.findAllByGroupRoomIdAndRegionKey(groupRoomId, regionKey, pageable)
        return buildListResponse(page, userId)
    }

    /** Page<Diary> → 댓글·좋아요 수를 채운 DiaryListResponse 로 변환(목록 응답 공통). */
    private fun buildListResponse(
        page: org.springframework.data.domain.Page<Diary>,
        userId: UUID
    ): DiaryListResponse {
        val diaries = page.content
        val diaryIds = diaries.map { it.id }

        val commentCountMap: Map<Long, Int> = if (diaryIds.isNotEmpty()) {
            commentRepository.countByTargetTypeAndTargetIdIn(CommentTargetType.DIARY, diaryIds)
                .associate { row -> (row[0] as Long) to (row[1] as Long).toInt() }
        } else {
            emptyMap()
        }

        val likeCountMap: Map<Long, Long> = if (diaryIds.isNotEmpty()) {
            diaryLikeRepository.countByDiaryIdIn(diaryIds)
                .associate { row -> (row[0] as Long) to (row[1] as Long) }
        } else {
            emptyMap()
        }

        val likedByMeSet: Set<Long> = if (diaryIds.isNotEmpty()) {
            diaryLikeRepository.findLikedDiaryIds(diaryIds, userId).toSet()
        } else {
            emptySet()
        }

        // 차단/신고 숨김 — 항목을 삭제하지 않고 본문만 비워 플래그를 단다(목록 자리·총개수 유지).
        val visibility = contentVisibilityService.forViewer(userId)

        return DiaryListResponse(
            diaries = diaries.map { diary ->
                val summary = DiarySummaryResponse.from(
                    diary = diary,
                    commentCount = commentCountMap[diary.id] ?: 0,
                    likeCount = likeCountMap[diary.id] ?: 0L,
                    likedByMe = diary.id in likedByMeSet
                )
                val reason = visibility.diaryHiddenReason(diary.id, diary.createdBy.id)
                if (reason != null) summary.asHidden(reason) else summary
            },
            total = page.totalElements
        )
    }

    override fun getDiaryCalendar(userId: UUID, groupRoomId: Long, month: YearMonth): DiaryCalendarResponse {
        ensureMember(userId, groupRoomId)
        val startDate = month.atDay(1)
        val endDate = month.atEndOfMonth()

        // 사진 그리드용: 해당 월의 모든 일기를 이미지까지 한 번에 로드.
        val monthDiaries = diaryRepository.findAllWithImagesByGroupRoomIdAndDateBetween(groupRoomId, startDate, endDate)

        // 차단/신고 숨김 — 일기를 지우지 않고 대표 칸만 숨김 표시. "하루 1편" 슬롯이 빈 날로 보이지 않게.
        val visibility = contentVisibilityService.forViewer(userId)

        // 날짜별 그룹핑. 같은 날 여러 편이면 최신(createdAt DESC, 쿼리에서 이미 정렬)을 대표로 사용.
        val byDate = monthDiaries.groupBy { it.date }
        val entries = byDate.entries
            .sortedBy { it.key }
            .map { (date, diaries) ->
                // 대표는 보이는 일기 우선. 전부 숨김이면 첫 일기를 대표로 두되 숨김 표시(슬롯·count 유지).
                val visible = diaries.firstOrNull {
                    visibility.diaryHiddenReason(it.id, it.createdBy.id) == null
                }
                val representative = visible ?: diaries.first()
                val entry = DiaryCalendarEntry(
                    date = date,
                    diaryId = representative.id,
                    thumbnailUrl = representative.images.minByOrNull { it.sortOrder }?.url,
                    mood = representative.mood,
                    count = diaries.size
                )
                if (visible != null) {
                    entry
                } else {
                    val reason = visibility.diaryHiddenReason(representative.id, representative.createdBy.id)
                        ?: VisibilityReason.HIDDEN
                    entry.asHidden(reason)
                }
            }
        val dates = entries.map { it.date }

        // 통계: 편수 / 최다 기분 / 연속 기록.
        val count = monthDiaries.size
        val topMood = monthDiaries
            .groupingBy { it.mood }
            .eachCount()
            .maxWithOrNull(compareBy({ it.value }, { -it.key }))
            ?.key
        val streak = computeStreak(groupRoomId)

        return DiaryCalendarResponse(
            dates = dates,
            entries = entries,
            stats = DiaryCalendarStats(count = count, streak = streak, topMood = topMood)
        )
    }

    /**
     * 오늘(KST) 기준 연속으로 일기를 쓴 일수를 계산한다.
     * 오늘 작성했으면 오늘부터, 아니면 어제부터 거슬러 올라가며 빈 날을 만나면 중단.
     * 월 경계를 넘을 수 있어 최근 [STREAK_WINDOW_DAYS] 일 범위의 작성 날짜 집합으로 판정한다.
     */
    private fun computeStreak(groupRoomId: Long): Int {
        val today = todayKst()
        val windowStart = today.minusDays(STREAK_WINDOW_DAYS)
        val writtenDays = diaryRepository
            .findDistinctDatesByGroupRoomIdAndMonth(groupRoomId, windowStart, today)
            .toHashSet()
        if (writtenDays.isEmpty()) return 0

        // 오늘 미작성이면 어제부터 시작(오늘은 아직 쓸 수 있으므로 streak 을 깨지 않음).
        var cursor = if (writtenDays.contains(today)) today else today.minusDays(1)
        var streak = 0
        while (writtenDays.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    override fun getDiaryDetail(userId: UUID, groupRoomId: Long, diaryId: Long): DiaryDetailResponse {
        ensureMember(userId, groupRoomId)

        val diary = diaryRepository.findById(diaryId)
            .orElseThrow { DigdaException(ErrorCode.DIARY_NOT_FOUND) }

        val comments = commentRepository
            .findAllByTargetTypeAndTargetIdOrderByCreatedAtAsc(CommentTargetType.DIARY, diaryId)

        val likeCount = diaryLikeRepository.countByDiaryId(diaryId)
        val likedByMe = diaryLikeRepository.existsByDiaryIdAndUserId(diaryId, userId)
        val reactions = buildReactionSummariesForOneDiary(diaryId, userId)

        // 차단/신고 숨김 — 본문/댓글을 비워 내려보낸다(상세 직접 접근 방어 + 차단 사용자 댓글 숨김).
        val visibility = contentVisibilityService.forViewer(userId)
        val diaryResponse = DiaryResponse.from(diary, likeCount, likedByMe, reactions).let { resp ->
            val reason = visibility.diaryHiddenReason(diary.id, diary.createdBy.id)
            if (reason != null) resp.asHidden(reason) else resp
        }

        return DiaryDetailResponse(
            diary = diaryResponse,
            comments = comments.map { comment ->
                val resp = DiaryCommentResponse.from(comment)
                val reason = visibility.commentHiddenReason(comment.id, comment.createdBy.id)
                if (reason != null) resp.asHidden(reason) else resp
            }
        )
    }

    @Transactional
    override fun createDiary(userId: UUID, groupRoomId: Long, request: CreateDiaryRequest): DiaryResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        if (request.date.isAfter(todayKst())) throw DigdaException(ErrorCode.FUTURE_DATE_NOT_ALLOWED)
        if (request.date.isBefore(editableFrom())) {
            log.info("action=일기 작성 거부(3개월 초과), userId={}, groupRoomId={}, date={}", userId, groupRoomId, request.date)
            throw DigdaException(ErrorCode.DIARY_DATE_TOO_OLD)
        }
        validateWeather(request.weather)
        validateMood(request.mood)
        validateImageCount(request.imageIds.size)

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        if (user.restricted) throw DigdaException(ErrorCode.USER_RESTRICTED)

        val resolvedUrls = resolveImageUrls(request.imageIds)

        val diary = Diary(
            groupRoom = groupRoom,
            title = request.title,
            content = request.content,
            date = request.date,
            weather = request.weather,
            mood = request.mood,
            location = request.location?.takeIf { it.isNotBlank() },
            regionKey = request.regionKey?.takeIf { it.isNotBlank() },
            regionSido = request.regionSido?.takeIf { it.isNotBlank() },
            regionSigungu = request.regionSigungu?.takeIf { it.isNotBlank() },
            createdBy = user
        )
        diary.replaceImages(resolvedUrls)
        val saved = diaryRepository.save(diary)

        groupRoom.updateLastActivity()

        notificationService.notifyDiaryWritten(groupRoomId, saved.id, userId, saved.title)

        userActionLogService.record(
            actorId = userId,
            action = UserAction.CREATE_DIARY,
            targetType = "DIARY",
            targetId = saved.id.toString(),
            detail = "groupRoomId=$groupRoomId, title=${saved.title}, images=${resolvedUrls.size}"
        )

        // 일기 작성 보상 — 모찌 경험치 지급. 그룹 공용 캐릭터에 누적되며, 같은 트랜잭션으로
        // 원자 처리한다(레벨업/진화 알림은 gainExp 내부에서 best-effort 로 처리됨).
        characterService.gainExp(
            userId = userId,
            groupRoomId = groupRoomId,
            amount = DIARY_WRITE_EXP,
            coinDelta = 0,
            source = "diary_write"
        )

        return DiaryResponse.from(saved, likeCount = 0L, likedByMe = false, reactions = emptyList())
    }

    @Transactional
    override fun updateDiary(
        userId: UUID,
        groupRoomId: Long,
        diaryId: Long,
        request: UpdateDiaryRequest
    ): DiaryResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val diary = diaryRepository.findById(diaryId)
            .orElseThrow { DigdaException(ErrorCode.DIARY_NOT_FOUND) }

        // 일기는 작성자 본인만 수정 가능 (방장도 불가 — 일정과 반대).
        if (diary.createdBy.id != userId) {
            throw DigdaException(ErrorCode.FORBIDDEN)
        }

        // 3개월이 지난 일기는 잠금 — 기존 날짜가 창을 벗어났거나 그 이전으로 바꾸려 하면 거부.
        if (diary.date.isBefore(editableFrom())) {
            log.info("action=일기 수정 거부(3개월 초과), userId={}, diaryId={}, date={}", userId, diaryId, diary.date)
            throw DigdaException(ErrorCode.DIARY_EDIT_WINDOW_EXPIRED)
        }

        request.date?.let {
            if (it.isAfter(todayKst())) throw DigdaException(ErrorCode.FUTURE_DATE_NOT_ALLOWED)
            if (it.isBefore(editableFrom())) throw DigdaException(ErrorCode.DIARY_DATE_TOO_OLD)
        }
        request.weather?.let { validateWeather(it) }
        request.mood?.let { validateMood(it) }
        request.imageIds?.let { validateImageCount(it.size) }

        diary.updateBasics(
            title = request.title,
            content = request.content,
            date = request.date,
            weather = request.weather,
            mood = request.mood,
            location = request.location?.takeIf { it.isNotBlank() },
            regionKey = request.regionKey?.takeIf { it.isNotBlank() },
            regionSido = request.regionSido?.takeIf { it.isNotBlank() },
            regionSigungu = request.regionSigungu?.takeIf { it.isNotBlank() }
        )
        request.imageIds?.let { ids ->
            diary.replaceImages(resolveImageUrls(ids))
        }

        groupRoom.updateLastActivity()

        val likeCount = diaryLikeRepository.countByDiaryId(diaryId)
        val likedByMe = diaryLikeRepository.existsByDiaryIdAndUserId(diaryId, userId)
        val reactions = buildReactionSummariesForOneDiary(diaryId, userId)

        return DiaryResponse.from(diary, likeCount, likedByMe, reactions)
    }

    @Transactional
    override fun deleteDiary(userId: UUID, groupRoomId: Long, diaryId: Long) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val diary = diaryRepository.findById(diaryId)
            .orElseThrow { DigdaException(ErrorCode.DIARY_NOT_FOUND) }

        // 일기는 작성자 본인만 삭제 가능 (방장도 불가 — 일정과 반대).
        if (diary.createdBy.id != userId) {
            throw DigdaException(ErrorCode.FORBIDDEN)
        }

        // 3개월이 지난 일기는 삭제도 잠근다(수정과 동일 정책).
        if (diary.date.isBefore(editableFrom())) {
            log.info("action=일기 삭제 거부(3개월 초과), userId={}, diaryId={}, date={}", userId, diaryId, diary.date)
            throw DigdaException(ErrorCode.DIARY_EDIT_WINDOW_EXPIRED)
        }

        val title = diary.title

        // diary 에 매달린 자식 레코드들을 먼저 정리해야 FK constraint 위반을 피한다.
        //   · diary_like / diary_reaction : diary_id FK + cascade 미설정 → 수동 삭제
        //   · comment : FK 는 없지만 target_type=DIARY 로 매핑돼있어 orphan 으로 남음 → 같이 삭제
        val likeDeleted = diaryLikeRepository.deleteAllByDiaryId(diaryId)
        val reactionDeleted = diaryReactionRepository.deleteAllByDiaryId(diaryId)
        val commentDeleted =
            commentRepository.deleteAllByTargetTypeAndTargetId(CommentTargetType.DIARY, diaryId)
        log.info(
            "diary 삭제 준비 - diaryId={}, likes={}, reactions={}, comments={}",
            diaryId,
            likeDeleted,
            reactionDeleted,
            commentDeleted
        )

        diaryRepository.delete(diary)

        userActionLogService.record(
            actorId = userId,
            action = UserAction.DELETE_DIARY,
            targetType = "DIARY",
            targetId = diaryId.toString(),
            detail = "groupRoomId=$groupRoomId, title=$title"
        )
    }

    @Transactional
    override fun toggleLike(userId: UUID, groupRoomId: Long, diaryId: Long): DiaryLikeResponse {
        ensureMember(userId, groupRoomId)
        val diary = diaryRepository.findById(diaryId)
            .orElseThrow { DigdaException(ErrorCode.DIARY_NOT_FOUND) }

        val alreadyLiked = diaryLikeRepository.existsByDiaryIdAndUserId(diaryId, userId)
        if (alreadyLiked) {
            diaryLikeRepository.deleteByDiaryIdAndUserId(diaryId, userId)
            log.info("action=일기 좋아요 취소, userId={}, diaryId={}", userId, diaryId)
        } else {
            val user = userRepository.findById(userId)
                .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }
            diaryLikeRepository.save(DiaryLike(diary = diary, user = user))
            log.info("action=일기 좋아요, userId={}, diaryId={}", userId, diaryId)
        }
        val newCount = diaryLikeRepository.countByDiaryId(diaryId)
        return DiaryLikeResponse(likedByMe = !alreadyLiked, likeCount = newCount)
    }

    @Transactional
    override fun toggleReaction(
        userId: UUID,
        groupRoomId: Long,
        diaryId: Long,
        request: ToggleDiaryReactionRequest
    ): DiaryReactionToggleResponse {
        ensureMember(userId, groupRoomId)
        val diary = diaryRepository.findById(diaryId)
            .orElseThrow { DigdaException(ErrorCode.DIARY_NOT_FOUND) }

        val type = request.type
        val exists = diaryReactionRepository.existsByDiaryIdAndUserIdAndType(diaryId, userId, type)
        if (exists) {
            diaryReactionRepository.deleteOne(diaryId, userId, type)
            log.info("action=일기 리액션 취소, userId={}, diaryId={}, type={}", userId, diaryId, type)
        } else {
            val user = userRepository.findById(userId)
                .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }
            diaryReactionRepository.save(DiaryReaction(diary = diary, user = user, type = type))
            log.info("action=일기 리액션, userId={}, diaryId={}, type={}", userId, diaryId, type)
        }

        val reactions = buildReactionSummariesForOneDiary(diaryId, userId)
        return DiaryReactionToggleResponse(reactions = reactions)
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private fun ensureMember(userId: UUID, groupRoomId: Long) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)
        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }
    }

    private fun validateWeather(weather: Int) {
        if (weather !in 0..3) throw DigdaException(ErrorCode.INVALID_WEATHER_VALUE)
    }

    private fun validateMood(mood: Int) {
        if (mood !in 0..4) throw DigdaException(ErrorCode.INVALID_MOOD_VALUE)
    }

    private fun validateImageCount(count: Int) {
        if (count > MAX_IMAGES_PER_DIARY) throw DigdaException(ErrorCode.FILE_TOO_LARGE)
    }

    /**
     * 클라이언트가 보낸 항목별로 처리하여 일기 image_url 리스트로 변환.
     *
     * - "http://" 또는 "https://" 로 시작하는 항목은 *기존 사진 URL* 로 간주하고 그대로 보존.
     *   (수정 시 보유 사진 일부만 추가/삭제할 수 있게 함)
     * - 그 외 숫자 문자열은 *신규 업로드 ID* 로 간주하고 UploadedImage 에서 lookup.
     * - 어느 쪽도 아니거나 lookup 실패 항목은 로그만 남기고 건너뜀.
     *
     * 순서는 입력 순서를 유지해 클라이언트가 정렬 순서를 결정한다.
     */
    private fun resolveImageUrls(imageIds: List<String>): List<String> {
        if (imageIds.isEmpty()) return emptyList()
        val urls = mutableListOf<String>()
        for (raw in imageIds) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                urls.add(trimmed)
                continue
            }
            val id = trimmed.toLongOrNull()
            if (id == null) {
                log.warn("imageIds 항목이 URL/Long 둘 다 아님: value={}", trimmed)
                continue
            }
            val img = uploadedImageRepository.findById(id).orElse(null)
            if (img == null) {
                log.warn("imageId 에 해당하는 업로드 레코드 없음: imageId={}", id)
                continue
            }
            urls.add(img.url)
        }
        return urls
    }

    private fun buildReactionSummariesForOneDiary(diaryId: Long, userId: UUID): List<DiaryReactionSummary> {
        val all = diaryReactionRepository.findAllByDiaryId(diaryId)
        if (all.isEmpty()) return emptyList()
        val counts: Map<DiaryReactionType, Int> = all.groupingBy { it.type }.eachCount()
        val mine: Set<DiaryReactionType> = all.filter { it.user.id == userId }.map { it.type }.toSet()
        return counts.entries
            .sortedBy { it.key.ordinal }
            .map { (type, count) ->
                DiaryReactionSummary(type = type, count = count, reactedByMe = type in mine)
            }
    }
}
