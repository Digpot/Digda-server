package digdaserver.domain.tapbattle.presentation.dto

import digdaserver.domain.tapbattle.domain.TapBattleGame
import java.util.UUID

/** 탭배틀 초대 생성 요청 — 같은 그룹의 상대 1명. */
data class CreateTapBattleRequest(
    val inviteeUserId: UUID
)

/** 누적 탭 수 보고 (STOMP `/app/tapbattle/{gameId}/taps`). */
data class TapReportRequest(
    val taps: Int
)

/** 대결 스냅샷 — REST 조회와 STOMP 이벤트가 공유. */
data class TapBattleResponse(
    val gameId: Long,
    val groupRoomId: Long,
    val status: TapBattleGame.Status,
    val inviterUserId: UUID,
    val inviterName: String,
    val inviteeUserId: UUID,
    val inviteeName: String,
    val inviterTaps: Int,
    val inviteeTaps: Int,
    /** 수락 시각 기준 카운트다운 시작/전투 종료 epoch ms — 클라이언트 로컬 타이머용. */
    val countdownStartEpochMs: Long?,
    val battleEndEpochMs: Long?,
    /** FINISHED 에서만. null 이면 무승부. */
    val winnerUserId: UUID?
) {
    companion object {
        fun from(game: TapBattleGame): TapBattleResponse = TapBattleResponse(
            gameId = game.id,
            groupRoomId = game.groupRoomId,
            status = game.status,
            inviterUserId = game.inviterId,
            inviterName = game.inviterName,
            inviteeUserId = game.inviteeId,
            inviteeName = game.inviteeName,
            inviterTaps = game.inviterTaps,
            inviteeTaps = game.inviteeTaps,
            countdownStartEpochMs = game.countdownStartAt?.toEpochMilli(),
            battleEndEpochMs = game.battleEndAt?.toEpochMilli(),
            winnerUserId = game.winnerId
        )
    }
}

/** `/topic/tapbattle/{gameId}` 브로드캐스트 이벤트 — 항상 스냅샷 포함. */
data class TapBattleEvent(
    val type: Type,
    val game: TapBattleResponse
) {
    enum class Type { ACCEPTED, DECLINED, CANCELED, SCORE, FINISHED, EXPIRED }
}
