package digdaserver.domain.molebattle.presentation.dto

import digdaserver.domain.molebattle.domain.MoleBattleGame
import java.util.UUID

/** 두더지 잡기 초대 생성 요청 — 같은 그룹의 상대 1명. */
data class CreateMoleBattleRequest(
    val inviteeUserId: UUID
)

/** 현재 점수 보고 (STOMP `/app/molebattle/{gameId}/score`). */
data class MoleScoreRequest(
    val score: Int
)

/** 대결 스냅샷 — REST 조회와 STOMP 이벤트가 공유. */
data class MoleBattleResponse(
    val gameId: Long,
    val groupRoomId: Long,
    val status: MoleBattleGame.Status,
    val inviterUserId: UUID,
    val inviterName: String,
    val inviteeUserId: UUID,
    val inviteeName: String,
    val inviterScore: Int,
    val inviteeScore: Int,
    /** 두더지 스폰 시드 — 양쪽이 같은 판을 재현. 수락 후에만. */
    val spawnSeed: Long?,
    val countdownStartEpochMs: Long?,
    val battleEndEpochMs: Long?,
    /** FINISHED 에서만. null 이면 무승부. */
    val winnerUserId: UUID?
) {
    companion object {
        fun from(game: MoleBattleGame): MoleBattleResponse = MoleBattleResponse(
            gameId = game.id,
            groupRoomId = game.groupRoomId,
            status = game.status,
            inviterUserId = game.inviterId,
            inviterName = game.inviterName,
            inviteeUserId = game.inviteeId,
            inviteeName = game.inviteeName,
            inviterScore = game.inviterScore,
            inviteeScore = game.inviteeScore,
            spawnSeed = game.spawnSeed,
            countdownStartEpochMs = game.countdownStartAt?.toEpochMilli(),
            battleEndEpochMs = game.battleEndAt?.toEpochMilli(),
            winnerUserId = game.winnerId
        )
    }
}

/** `/topic/molebattle/{gameId}` 브로드캐스트 이벤트 — 항상 스냅샷 포함. */
data class MoleBattleEvent(
    val type: Type,
    val game: MoleBattleResponse
) {
    enum class Type { ACCEPTED, DECLINED, CANCELED, SCORE, FINISHED, EXPIRED }
}
