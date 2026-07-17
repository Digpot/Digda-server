package digdaserver.domain.catchmind.presentation.dto

import digdaserver.domain.catchmind.domain.CatchmindGame
import java.util.UUID

/** 방 생성 요청 — 같은 그룹의 초대 대상 목록(방장 제외, 1명 이상). */
data class CreateCatchmindRequest(
    val inviteeUserIds: List<UUID>
)

/** 추리 채팅 (STOMP `/app/catchmind/{gameId}/guess`). */
data class CatchmindGuessRequest(
    val text: String
)

/**
 * 획 조각 (STOMP `/app/catchmind/{gameId}/stroke`).
 * 좌표는 캔버스 기준 0..1 정규화. [done]=false 면 같은 [strokeId] 의 후속 조각이 이어진다.
 */
data class CatchmindStrokeRequest(
    val strokeId: Long,
    val color: String,
    val width: Double,
    val points: List<List<Double>>,
    val done: Boolean
)

data class CatchmindPlayerDto(
    val userId: UUID,
    val name: String,
    val joined: Boolean,
    val declined: Boolean,
    val score: Int,
    val isHost: Boolean
) {
    companion object {
        fun from(p: CatchmindGame.Player, hostId: UUID): CatchmindPlayerDto =
            CatchmindPlayerDto(
                userId = p.userId,
                name = p.name,
                joined = p.joined,
                declined = p.declined,
                score = p.score,
                isHost = p.userId == hostId
            )
    }
}

/**
 * 방/게임 스냅샷 — REST 조회와 STOMP 이벤트가 공유.
 * [word] 는 요청자가 현재 출제자일 때만 채워진다(그 외 null — 정답 유출 방지).
 * [wordLength] 는 추리자 힌트(글자 수). [strokes] 는 현재 라운드 누적 획(재접속 리플레이).
 */
data class CatchmindGameResponse(
    val gameId: Long,
    val groupRoomId: Long,
    val status: CatchmindGame.Status,
    val hostUserId: UUID,
    val hostName: String,
    val players: List<CatchmindPlayerDto>,
    val roundIndex: Int,
    val totalRounds: Int,
    val drawerUserId: UUID?,
    val word: String?,
    val wordLength: Int?,
    val roundDeadlineEpochMs: Long?,
    val strokes: List<CatchmindGame.Stroke>
) {
    companion object {
        fun from(
            game: CatchmindGame,
            forUserId: UUID?,
            includeStrokes: Boolean = false
        ): CatchmindGameResponse = CatchmindGameResponse(
            gameId = game.id,
            groupRoomId = game.groupRoomId,
            status = game.status,
            hostUserId = game.hostId,
            hostName = game.hostName,
            players = game.players.values.map { CatchmindPlayerDto.from(it, game.hostId) },
            roundIndex = game.roundIndex,
            totalRounds = game.totalRounds,
            drawerUserId = game.drawerId,
            word = if (forUserId != null && forUserId == game.drawerId) game.word else null,
            wordLength = game.word?.length,
            roundDeadlineEpochMs = game.roundDeadline?.toEpochMilli(),
            strokes = if (includeStrokes) game.strokes.toList() else emptyList()
        )
    }
}

/**
 * `/topic/catchmind/{gameId}` 브로드캐스트 이벤트.
 * STROKE/CLEAR 는 [stroke] 만 실어 가볍게, 상태 변화 이벤트는 [game] 스냅샷을 싣는다
 * (브로드캐스트 스냅샷의 word 는 항상 null — 정답은 라운드 종료 이벤트의 [answer] 로만).
 */
data class CatchmindEvent(
    val type: Type,
    val game: CatchmindGameResponse? = null,
    val stroke: CatchmindGame.Stroke? = null,
    /** GUESS(오답 채팅)/CORRECT 의 발화자. */
    val userId: UUID? = null,
    val userName: String? = null,
    /** GUESS 의 채팅 본문. */
    val text: String? = null,
    /** CORRECT/ROUND_TIMEOUT/ROUND_SKIPPED 에서 공개되는 정답. */
    val answer: String? = null
) {
    enum class Type {
        PLAYER_JOINED, PLAYER_DECLINED, STARTED, ROUND_START,
        STROKE, CLEAR, GUESS, CORRECT, ROUND_TIMEOUT, ROUND_SKIPPED,
        FINISHED, CANCELED, EXPIRED
    }
}
