package digdaserver.domain.omok.presentation.dto

import digdaserver.domain.omok.domain.OmokGame
import java.util.UUID

/** 오목 초대 생성 요청 — 같은 그룹의 상대 1명. */
data class CreateOmokGameRequest(
    val inviteeUserId: UUID
)

/** 착수 요청 (STOMP `/app/omok/{gameId}/move`). 좌표는 0-based. */
data class OmokMoveRequest(
    val x: Int,
    val y: Int
)

/**
 * 대국 스냅샷 — REST 조회와 STOMP 이벤트가 공유하는 단일 뷰.
 * 이벤트마다 전체 보드를 실어 클라이언트 재동기화(재접속 포함)를 단순하게 만든다.
 */
data class OmokGameResponse(
    val gameId: Long,
    val groupRoomId: Long,
    val status: OmokGame.Status,
    val inviterUserId: UUID,
    val inviterName: String,
    val inviteeUserId: UUID,
    val inviteeName: String,
    /** 15×15, 0=빈칸 / 1=흑(초대자) / 2=백(수락자). board[y][x]. */
    val board: List<List<Int>>,
    val currentTurnUserId: UUID?,
    val winnerUserId: UUID?,
    val finishReason: OmokGame.FinishReason?,
    val winningLine: List<List<Int>>
) {
    companion object {
        fun from(game: OmokGame): OmokGameResponse = OmokGameResponse(
            gameId = game.id,
            groupRoomId = game.groupRoomId,
            status = game.status,
            inviterUserId = game.inviterId,
            inviterName = game.inviterName,
            inviteeUserId = game.inviteeId,
            inviteeName = game.inviteeName,
            board = game.board.map { it.toList() },
            currentTurnUserId =
            if (game.status == OmokGame.Status.ACTIVE) game.currentTurnId else null,
            winnerUserId = game.winnerId,
            finishReason = game.finishReason,
            winningLine = game.winningLine
        )
    }
}

/**
 * `/topic/omok/{gameId}` 로 브로드캐스트되는 이벤트.
 * [game] 스냅샷이 항상 실려 클라이언트는 이 한 페이로드로 화면을 전부 갱신한다.
 */
data class OmokEvent(
    val type: Type,
    val game: OmokGameResponse,
    /** MOVE 이벤트의 마지막 착수 — [x, y, stone]. 그 외 null. */
    val lastMove: List<Int>? = null
) {
    enum class Type { ACCEPTED, DECLINED, CANCELED, MOVE, FINISHED, EXPIRED }
}
