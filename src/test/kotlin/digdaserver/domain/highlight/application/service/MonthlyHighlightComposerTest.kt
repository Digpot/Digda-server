package digdaserver.domain.highlight.application.service

import digdaserver.domain.highlight.application.service.MonthlyHighlightComposer.DiaryInput
import digdaserver.domain.highlight.application.service.MonthlyHighlightComposer.MemberInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthlyHighlightComposerTest {

    private val oct = YearMonth.of(2026, 10)
    private val members = listOf(
        MemberInput("a", "가은", null),
        MemberInput("b", "나래", null),
        MemberInput("c", "다온", null)
    )

    private fun diary(
        id: Long,
        day: Int,
        author: String = "a",
        images: List<String> = emptyList(),
        location: String? = null,
        likes: Int = 0,
        reactions: Int = 0,
        comments: Int = 0,
        content: String = "내용$id"
    ) = DiaryInput(
        id = id,
        title = "제목$id",
        content = content,
        date = LocalDate.of(2026, 10, day),
        authorId = author,
        authorName = "작성자$author",
        authorImage = null,
        imageUrls = images,
        location = location,
        likeCount = likes,
        reactionCount = reactions,
        commentCount = comments
    )

    @Test
    fun `일기가 없으면 하이라이트를 만들지 않는다`() {
        assertNull(MonthlyHighlightComposer.compose(oct, emptyList(), members))
    }

    @Test
    fun `구성원별 개수와 최다 작성자를 집계하고 0편 멤버도 포함한다`() {
        val p = MonthlyHighlightComposer.compose(
            oct,
            listOf(diary(1, 1, "a"), diary(2, 2, "a"), diary(3, 3, "b")),
            members
        )!!
        assertEquals(3, p.totalDiaryCount)
        assertEquals(listOf("a", "b", "c"), p.members.map { it.userId })
        assertEquals(listOf(2, 1, 0), p.members.map { it.diaryCount })
        assertEquals(listOf("a"), p.topWriters.map { it.userId })
        assertEquals(31, p.daysInMonth)
    }

    @Test
    fun `최다 작성자가 동률이면 모두 포함한다`() {
        val p = MonthlyHighlightComposer.compose(oct, listOf(diary(1, 1, "a"), diary(2, 2, "b")), members)!!
        assertEquals(setOf("a", "b"), p.topWriters.map { it.userId }.toSet())
    }

    @Test
    fun `탈퇴했지만 지난달에 쓴 작성자도 기록에 남는다`() {
        val p = MonthlyHighlightComposer.compose(oct, listOf(diary(1, 1, "ghost")), members)!!
        assertTrue(p.members.any { it.userId == "ghost" && it.diaryCount == 1 })
    }

    @Test
    fun `대표 일기는 반응이 많은 순이고 사진은 일기당 한 장만 쓴다`() {
        val p = MonthlyHighlightComposer.compose(
            oct,
            listOf(
                diary(1, 1, images = listOf("x1", "x2")),
                diary(2, 2, images = listOf("y1"), likes = 2, comments = 1), // engagement 4
                diary(3, 3, reactions = 1) // engagement 1, 사진 없음
            ),
            members
        )!!
        assertEquals(listOf(2L, 3L, 1L), p.featuredDiaries.map { it.diaryId })
        assertEquals(listOf("y1", "x1"), p.photos)
        assertEquals("y1", p.coverImageUrl)
    }

    @Test
    fun `사진이 없으면 대표 사진은 null 이고 지어내지 않는다`() {
        val p = MonthlyHighlightComposer.compose(oct, listOf(diary(1, 1)), members)!!
        assertNull(p.coverImageUrl)
        assertTrue(p.photos.isEmpty())
        assertNull(p.topLocation)
    }

    @Test
    fun `활동일 수 최다 장소 최다 요일을 계산한다`() {
        // 2026-10-05 월, 10-06 화, 10-12 월
        val p = MonthlyHighlightComposer.compose(
            oct,
            listOf(
                diary(1, 5, location = "제주"),
                diary(2, 5, "b", location = "제주"),
                diary(3, 6, location = "서울"),
                diary(4, 12, location = " ")
            ),
            members
        )!!
        assertEquals(3, p.activeDays)
        assertEquals("제주", p.topLocation)
        assertEquals(1, p.busiestWeekday)
        assertTrue(p.summary.contains("4편"))
    }

    @Test
    fun `긴 본문은 발췌로 줄인다`() {
        val p = MonthlyHighlightComposer.compose(oct, listOf(diary(1, 1, content = "가".repeat(200))), members)!!
        assertTrue(p.featuredDiaries.first().excerpt.length <= 91)
        assertTrue(p.featuredDiaries.first().excerpt.endsWith("…"))
    }

    @Test
    fun `대표 일기는 최대 3개 사진은 최대 6장`() {
        val many = (1L..10L).map { diary(it, it.toInt(), images = listOf("p$it")) }
        val p = MonthlyHighlightComposer.compose(oct, many, members)!!
        assertEquals(MonthlyHighlightComposer.MAX_FEATURED, p.featuredDiaries.size)
        assertEquals(MonthlyHighlightComposer.MAX_PHOTOS, p.photos.size)
    }
}
