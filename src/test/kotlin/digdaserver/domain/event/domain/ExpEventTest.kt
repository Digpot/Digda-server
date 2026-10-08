package digdaserver.domain.event.domain

import digdaserver.domain.event.domain.entity.ExpEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * 경험치 배수 이벤트의 기간 판정과 배수 적용 검증.
 *
 * 이벤트가 "언제부터 언제까지" 인지 잘못 판정하면 추석이 끝난 뒤에도 2배가 계속 나가거나
 * 시작 당일 아침에 안 나가는 사고로 직결돼서, 경계 시각을 특히 촘촘히 본다.
 */
class ExpEventTest {

    private val start = LocalDateTime.of(2026, 9, 24, 0, 0)
    private val end = LocalDateTime.of(2026, 10, 1, 0, 0)

    private fun event(
        enabled: Boolean = true,
        multiplier: Double = 2.0,
        startAt: LocalDateTime? = start,
        endAt: LocalDateTime? = end
    ) = ExpEvent(
        enabled = enabled,
        title = "추석 2배",
        multiplier = multiplier,
        startAt = startAt,
        endAt = endAt
    )

    @Test
    fun `시작 시각은 포함되고 종료 시각은 제외된다`() {
        val e = event()
        assertFalse(e.isActiveAt(start.minusSeconds(1)))
        assertTrue(e.isActiveAt(start))
        assertTrue(e.isActiveAt(end.minusSeconds(1)))
        assertFalse(e.isActiveAt(end))
    }

    @Test
    fun `기간이 비어 있으면 그 방향 제한이 없다`() {
        assertTrue(event(startAt = null, endAt = null).isActiveAt(LocalDateTime.of(2020, 1, 1, 0, 0)))
        assertTrue(event(startAt = null).isActiveAt(start.minusYears(1)))
        assertFalse(event(endAt = null).isActiveAt(start.minusSeconds(1)))
        assertTrue(event(endAt = null).isActiveAt(end.plusYears(1)))
    }

    @Test
    fun `꺼져 있거나 배수가 1배면 기간 안이어도 비활성`() {
        assertFalse(event(enabled = false).isActiveAt(start))
        assertFalse(event(multiplier = 1.0).isActiveAt(start))
        assertEquals(1.0, event(enabled = false).multiplierAt(start))
    }

    @Test
    fun `비활성 구간에서는 경험치가 그대로다`() {
        val e = event()
        assertEquals(30, e.applyTo(30, end))
        assertEquals(60, e.applyTo(30, start))
    }

    @Test
    fun `소수 배수는 반올림하고 원본보다 작아지지 않는다`() {
        val e = event(multiplier = 1.5)
        assertEquals(8, e.applyTo(5, start)) // 7.5 → 8
        assertEquals(2, e.applyTo(1, start)) // 1.5 → 2
        assertEquals(0, e.applyTo(0, start))
    }

    @Test
    fun `update 는 배수를 허용 범위로 clamp 한다`() {
        val e = event()
        e.update(enabled = true, title = "과욕", multiplier = 99.0, startAt = null, endAt = null)
        assertEquals(ExpEvent.MAX_MULTIPLIER, e.multiplier)

        e.update(enabled = true, title = "음수", multiplier = -3.0, startAt = null, endAt = null)
        assertEquals(ExpEvent.MIN_MULTIPLIER, e.multiplier)
    }
}
