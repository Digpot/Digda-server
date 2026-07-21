package digdaserver.domain.alkkagi.presentation.dto

import digdaserver.domain.alkkagi.domain.AlkkagiGame
import java.util.UUID

/**
 * 알까기 초대 생성 요청 — 같은 그룹의 상대 1명 + 돌 개수(1~10) + 내 시작 대형.
 * [formation] 은 LINE/DOUBLE/WEDGE/DIAMOND/SIDES 중 하나(기본 LINE).
 */
data class CreateAlkkagiGameRequest(
    val inviteeUserId: UUID,
    val stoneCount: Int = 5,
    val formation: String? = null
)

/** 초대 수락 요청 — 수락자 자신의 시작 대형(기본 LINE). */
data class AcceptAlkkagiGameRequest(
    val formation: String? = null
)

/** 돌 스냅샷/업데이트 공용 표현 — 좌표는 0..1 정규화. */
data class AlkkagiStoneDto(
    val id: Int,
    val owner: Int,
    val x: Double,
    val y: Double,
    val alive: Boolean
) {
    companion object {
        fun from(stone: AlkkagiGame.Stone): AlkkagiStoneDto = AlkkagiStoneDto(
            id = stone.id,
            owner = stone.owner,
            x = stone.x,
            y = stone.y,
            alive = stone.alive
        )
    }
}

/**
 * 수 반영 요청 (STOMP `/app/alkkagi/{gameId}/move`).
 *
 * 물리 시뮬은 튕긴 클라이언트가 수행하고, 종료 시점의 돌 최종 상태([stones])와
 * 상대 화면 재생용 초기 속도([flickVx]/[flickVy], 정규화 좌표계 단위/초)를 보낸다.
 */
data class AlkkagiMoveRequest(
    val stoneId: Int,
    val flickVx: Double,
    val flickVy: Double,
    val stones: List<AlkkagiStoneDto>
)

/**
 * 대국 스냅샷 — REST 조회와 STOMP 이벤트가 공유하는 단일 뷰.
 * 이벤트마다 전체 돌 상태를 실어 클라이언트 재동기화(재접속 포함)를 단순하게 만든다.
 */
data class AlkkagiGameResponse(
    val gameId: Long,
    val groupRoomId: Long,
    val status: AlkkagiGame.Status,
    val inviterUserId: UUID,
    val inviterName: String,
    val inviteeUserId: UUID,
    val inviteeName: String,
    /** 한 쪽당 돌 개수(1~10) — 초대자가 설정. */
    val stoneCount: Int,
    /** 보드 세로 길이(가로=1 기준) — 클라 렌더/낙사 판정 공유 상수. */
    val boardHeight: Double,
    val stones: List<AlkkagiStoneDto>,
    val currentTurnUserId: UUID?,
    /** 현재 턴 마감 시각(epoch ms) — 30초 제한. ACTIVE 가 아니면 null. */
    val turnDeadlineEpochMs: Long?,
    val winnerUserId: UUID?,
    val finishReason: AlkkagiGame.FinishReason?
) {
    companion object {
        fun from(game: AlkkagiGame): AlkkagiGameResponse = AlkkagiGameResponse(
            gameId = game.id,
            groupRoomId = game.groupRoomId,
            status = game.status,
            inviterUserId = game.inviterId,
            inviterName = game.inviterName,
            inviteeUserId = game.inviteeId,
            inviteeName = game.inviteeName,
            stoneCount = game.stoneCount,
            boardHeight = AlkkagiGame.BOARD_HEIGHT,
            stones = game.stones.map { AlkkagiStoneDto.from(it) },
            currentTurnUserId =
            if (game.status == AlkkagiGame.Status.ACTIVE) game.currentTurnId else null,
            turnDeadlineEpochMs =
            if (game.status == AlkkagiGame.Status.ACTIVE) {
                game.turnStartedAt.plusSeconds(AlkkagiGame.TURN_LIMIT_SECONDS).toEpochMilli()
            } else {
                null
            },
            winnerUserId = game.winnerId,
            finishReason = game.finishReason
        )
    }
}

/**
 * `/topic/alkkagi/{gameId}` 로 브로드캐스트되는 이벤트.
 * [game] 스냅샷이 항상 실려 클라이언트는 이 한 페이로드로 화면을 전부 갱신한다.
 * MOVE/FINISHED 에는 상대 화면 애니메이션 재생용 [flick] 이 함께 실린다.
 */
data class AlkkagiEvent(
    val type: Type,
    val game: AlkkagiGameResponse,
    val flick: Flick? = null
) {
    enum class Type { ACCEPTED, DECLINED, CANCELED, MOVE, TIMEOUT, FINISHED, EXPIRED }

    /** 마지막으로 튕긴 돌과 초기 속도 — 정규화 좌표계 단위/초. */
    data class Flick(
        val byUserId: UUID,
        val stoneId: Int,
        val vx: Double,
        val vy: Double
    )
}
