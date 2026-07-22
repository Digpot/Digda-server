package digdaserver.domain.alkkagi.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.math.hypot

/**
 * 알까기 시작 배치 검증 — 대형이 실제로 적용되는지, 그리고 돌 1~10개 어떤
 * 조합에서도 돌이 판 밖으로 넘치거나 서로 겹치지 않는지 전수 확인한다.
 */
class AlkkagiGameTest {

    /** 클라이언트 물리와 공유하는 돌 반지름(정규화 좌표계). */
    private val stoneRadius = 0.045

    private fun newGame(
        stoneCount: Int,
        inviterFormation: AlkkagiGame.Formation
    ): AlkkagiGame = AlkkagiGame(
        id = 1L,
        groupRoomId = 1L,
        inviterId = UUID.randomUUID(),
        inviterName = "inviter",
        inviteeId = UUID.randomUUID(),
        inviteeName = "invitee",
        stoneCountRequested = stoneCount,
        inviterFormation = inviterFormation
    )

    @Test
    fun `초대자 대형이 실제 배치에 반영된다`() {
        for (count in 1..AlkkagiGame.MAX_STONES) {
            val placements = AlkkagiGame.Formation.entries.associateWith { formation ->
                newGame(count, formation).stones.map { it.x to it.y }.toSet()
            }
            // 돌 2개 이상이면 다섯 대형의 배치가 전부 서로 달라야 한다.
            if (count >= 2) {
                assertEquals(
                    AlkkagiGame.Formation.entries.size,
                    placements.values.toSet().size,
                    "count=$count 에서 대형별 배치가 구분되지 않음"
                )
            }
        }
    }

    @Test
    fun `수락자 대형도 실제 배치에 반영된다`() {
        val base = newGame(5, AlkkagiGame.Formation.LINE)
        val wedge = newGame(5, AlkkagiGame.Formation.LINE)
        base.accept(base.inviteeId, AlkkagiGame.Formation.LINE)
        wedge.accept(wedge.inviteeId, AlkkagiGame.Formation.WEDGE)
        val inviteeOf = { g: AlkkagiGame ->
            g.stones.filter { it.owner == AlkkagiGame.INVITEE }.map { it.x to it.y }.toSet()
        }
        assertNotEquals(inviteeOf(base), inviteeOf(wedge))
    }

    @Test
    fun `모든 대형 x 돌 1~10개 조합에서 돌이 판 안에 있고 겹치지 않는다`() {
        for (inviterF in AlkkagiGame.Formation.entries) {
            for (inviteeF in AlkkagiGame.Formation.entries) {
                for (count in AlkkagiGame.MIN_STONES..AlkkagiGame.MAX_STONES) {
                    val game = newGame(count, inviterF)
                    game.accept(game.inviteeId, inviteeF)
                    val label = "inviter=$inviterF, invitee=$inviteeF, count=$count"

                    assertEquals(count * 2, game.stones.size, label)
                    assertTrue(game.stones.all { it.alive }, label)

                    // 판 경계 — 반지름까지 포함해 완전히 판 안.
                    for (s in game.stones) {
                        assertTrue(
                            s.x in stoneRadius..(1.0 - stoneRadius) &&
                                s.y in stoneRadius..(AlkkagiGame.BOARD_HEIGHT - stoneRadius),
                            "$label — 돌 ${s.id} (${s.x}, ${s.y}) 가 판을 넘침"
                        )
                    }

                    // 겹침 — 서로 다른 돌의 중심 거리가 지름 이상.
                    for (i in game.stones.indices) {
                        for (j in i + 1 until game.stones.size) {
                            val a = game.stones[i]
                            val b = game.stones[j]
                            val d = hypot(a.x - b.x, a.y - b.y)
                            assertTrue(
                                d >= stoneRadius * 2 - 1e-9,
                                "$label — 돌 ${a.id}·${b.id} 겹침 (거리 $d)"
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `양쪽 진영이 중앙선을 침범하지 않는다`() {
        val mid = AlkkagiGame.BOARD_HEIGHT / 2
        for (f in AlkkagiGame.Formation.entries) {
            val game = newGame(AlkkagiGame.MAX_STONES, f)
            game.accept(game.inviteeId, f)
            for (s in game.stones) {
                if (s.owner == AlkkagiGame.INVITER) {
                    assertTrue(s.y - stoneRadius > mid, "formation=$f 초대자 돌이 중앙선 침범")
                } else {
                    assertTrue(s.y + stoneRadius < mid, "formation=$f 수락자 돌이 중앙선 침범")
                }
            }
        }
    }
}
