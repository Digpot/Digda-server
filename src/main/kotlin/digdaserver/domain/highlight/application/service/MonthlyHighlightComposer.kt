package digdaserver.domain.highlight.application.service

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * 지난달 일기 데이터 → [HighlightPayload]. DB·시간에 의존하지 않는 순수 로직이라 단위 테스트가 쉽다.
 * 입력에 실제로 있는 데이터만 사용하고, 없는 항목(사진·장소 등)은 null/빈 목록으로 둔다.
 * AI 호출 없이 규칙 기반 문장으로 요약해 외부 API 비용이 들지 않는다.
 */
object MonthlyHighlightComposer {

    const val MAX_FEATURED = 3
    const val MAX_PHOTOS = 6
    private const val EXCERPT_LENGTH = 90

    data class DiaryInput(
        val id: Long,
        val title: String,
        val content: String,
        val date: LocalDate,
        val authorId: String,
        val authorName: String,
        val authorImage: String?,
        /** sort_order 순. */
        val imageUrls: List<String>,
        val location: String?,
        val likeCount: Int,
        val reactionCount: Int,
        val commentCount: Int
    ) {
        val engagement: Int get() = likeCount + reactionCount + commentCount * 2
    }

    data class MemberInput(val userId: String, val name: String, val profileImage: String?)

    /** 일기가 없으면 null — 호출 측에서 하이라이트를 만들지 않는다. */
    fun compose(month: YearMonth, diaries: List<DiaryInput>, members: List<MemberInput>): HighlightPayload? {
        if (diaries.isEmpty()) return null

        val countByAuthor = diaries.groupingBy { it.authorId }.eachCount()
        val memberStats = buildMemberStats(diaries, members, countByAuthor)
        val topCount = memberStats.maxOf { it.diaryCount }
        val topWriters = memberStats.filter { it.diaryCount == topCount }

        // 대표 일기: 반응(좋아요·이모지·댓글) 많은 순 → 사진 있는 순 → 긴 글 → 먼저 쓴 순(id).
        val ranked = diaries.sortedWith(
            compareByDescending<DiaryInput> { it.engagement }
                .thenByDescending { it.imageUrls.isNotEmpty() }
                .thenByDescending { it.content.length }
                .thenBy { it.id }
        )
        val featured = ranked.take(MAX_FEATURED).map { it.toFeatured() }
        val photos = ranked.filter { it.imageUrls.isNotEmpty() }.take(MAX_PHOTOS).map { it.imageUrls.first() }

        val activeDays = diaries.map { it.date }.distinct().size
        val topLocation = diaries.mapNotNull { it.location?.trim()?.takeIf(String::isNotEmpty) }
            .groupingBy { it }.eachCount()
            .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()?.key
        val busiestWeekday = diaries.groupingBy { it.date.dayOfWeek.value }.eachCount()
            .entries.sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()?.key

        return HighlightPayload(
            totalDiaryCount = diaries.size,
            activeDays = activeDays,
            daysInMonth = month.lengthOfMonth(),
            members = memberStats,
            topWriters = topWriters,
            coverImageUrl = photos.firstOrNull(),
            photos = photos,
            featuredDiaries = featured,
            topLocation = topLocation,
            busiestWeekday = busiestWeekday,
            summary = buildSummary(month, diaries.size, activeDays, topWriters, topCount, topLocation, busiestWeekday),
            closingMessage = closingMessage(month, diaries.size)
        )
    }

    private fun buildMemberStats(
        diaries: List<DiaryInput>,
        members: List<MemberInput>,
        countByAuthor: Map<String, Int>
    ): List<HighlightMemberStat> {
        val stats = LinkedHashMap<String, HighlightMemberStat>()
        members.forEach {
            stats[it.userId] = HighlightMemberStat(it.userId, it.name, it.profileImage, countByAuthor[it.userId] ?: 0)
        }
        // 지금은 그룹을 떠났지만 지난달에 쓴 사람도 기록에는 남긴다.
        diaries.forEach {
            stats.getOrPut(it.authorId) {
                HighlightMemberStat(it.authorId, it.authorName, it.authorImage, countByAuthor[it.authorId] ?: 0)
            }
        }
        return stats.values.sortedWith(compareByDescending<HighlightMemberStat> { it.diaryCount }.thenBy { it.name })
    }

    private fun DiaryInput.toFeatured() = HighlightDiary(
        diaryId = id,
        title = title,
        excerpt = content.replace(Regex("\\s+"), " ").trim().let {
            if (it.length > EXCERPT_LENGTH) it.take(EXCERPT_LENGTH).trimEnd() + "…" else it
        },
        date = date.toString(),
        authorName = authorName,
        imageUrl = imageUrls.firstOrNull(),
        likeCount = likeCount,
        reactionCount = reactionCount,
        commentCount = commentCount
    )

    private fun buildSummary(
        month: YearMonth,
        total: Int,
        activeDays: Int,
        topWriters: List<HighlightMemberStat>,
        topCount: Int,
        topLocation: String?,
        busiestWeekday: Int?
    ): String {
        val parts = mutableListOf<String>()
        parts += "${month.monthValue}월에는 ${activeDays}일 동안 모두 ${total}편의 일기가 쌓였어요."
        val names = topWriters.joinToString(", ") { "${it.name}님" }
        parts += if (topWriters.size == 1) {
            "${names}이 ${topCount}편으로 가장 많은 추억을 남겼어요."
        } else {
            "${names}이 각각 ${topCount}편씩 가장 열심히 기록했어요."
        }
        if (topLocation != null) parts += "가장 자주 기록된 장소는 '$topLocation'이에요."
        if (busiestWeekday != null) {
            parts += "${koreanWeekday(busiestWeekday)}요일에 가장 많은 이야기가 모였어요."
        }
        return parts.joinToString(" ")
    }

    private fun closingMessage(month: YearMonth, total: Int): String {
        val next = month.plusMonths(1).monthValue
        return when {
            total >= 20 -> "${month.monthValue}월은 정말 빼곡한 한 달이었어요. ${next}월에도 함께 이야기를 채워가요!"
            total >= 5 -> "${month.monthValue}월의 소중한 순간들이 차곡차곡 모였어요. ${next}월에도 함께해요!"
            else -> "짧아도 소중한 ${month.monthValue}월의 기록이에요. ${next}월에는 더 많은 추억을 남겨봐요!"
        }
    }

    private fun koreanWeekday(isoValue: Int): String = when (DayOfWeek.of(isoValue)) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }
}
