package digdaserver.domain.wordchain.presentation.dto

import digdaserver.domain.wordchain.domain.WordChainGame
import java.util.UUID

/**
 * 방 생성 요청 — 초대 대상 목록(방장 제외, 1명 이상)
 * + 턴 제한시간(서버에서 10~60초 클램프).
 */
data class CreateWordChainRequest(
    val inviteeUserIds: List<UUID>,
    val turnSeconds: Int = WordChainGame.DEFAULT_TURN_SECONDS
)

/** 단어 제출 (STOMP `/app/wordchain/{gameId}/word`). */
data class WordSubmitRequest(
    val word: String
)

data class WordChainPlayerDto(
    val userId: UUID,
    val name: String,
    val joined: Boolean,
    val declined: Boolean,
    val eliminated: Boolean,
    val wordCount: Int,
    val isHost: Boolean
) {
    companion object {
        fun from(p: WordChainGame.Player, hostId: UUID): WordChainPlayerDto =
            WordChainPlayerDto(
                userId = p.userId,
                name = p.name,
                joined = p.joined,
                declined = p.declined,
                eliminated = p.eliminated,
                wordCount = p.wordCount,
                isHost = p.userId == hostId
            )
    }
}

data class WordChainEntryDto(
    val userId: UUID?,
    val userName: String?,
    val word: String
) {
    companion object {
        fun from(e: WordChainGame.Entry): WordChainEntryDto =
            WordChainEntryDto(userId = e.userId, userName = e.userName, word = e.word)
    }
}

/** 게임 스냅샷 — REST 조회와 STOMP 이벤트가 공유. 단어 기록 전체 포함(재접속 복원). */
data class WordChainResponse(
    val gameId: Long,
    val groupRoomId: Long,
    val status: WordChainGame.Status,
    val hostUserId: UUID,
    val hostName: String,
    val players: List<WordChainPlayerDto>,
    val entries: List<WordChainEntryDto>,
    val currentTurnUserId: UUID?,
    val turnDeadlineEpochMs: Long?,
    val turnSeconds: Int,
    val winnerUserId: UUID?
) {
    companion object {
        fun from(game: WordChainGame): WordChainResponse = WordChainResponse(
            gameId = game.id,
            groupRoomId = game.groupRoomId,
            status = game.status,
            hostUserId = game.hostId,
            hostName = game.hostName,
            players = game.players.values.map { WordChainPlayerDto.from(it, game.hostId) },
            entries = game.entries.map { WordChainEntryDto.from(it) },
            currentTurnUserId = game.currentTurnId,
            turnDeadlineEpochMs = game.turnDeadline?.toEpochMilli(),
            turnSeconds = game.turnTime.seconds.toInt(),
            winnerUserId = game.winnerId
        )
    }
}

/**
 * `/topic/wordchain/{gameId}` 브로드캐스트 이벤트.
 * WORD(수락)/TIMEOUT(탈락)은 스냅샷 포함, REJECTED 는 제출자 표시용 가벼운 이벤트.
 */
data class WordChainEvent(
    val type: Type,
    val game: WordChainResponse? = null,
    /** WORD/REJECTED/TIMEOUT 의 대상 사용자. */
    val userId: UUID? = null,
    val userName: String? = null,
    /** WORD 의 수락 단어 / REJECTED 의 반려 단어. */
    val word: String? = null,
    /** REJECTED 사유 — RULE(끝글자)/USED(중복)/FORMAT(형식). */
    val reason: String? = null
) {
    enum class Type {
        PLAYER_JOINED, PLAYER_DECLINED, STARTED, WORD, REJECTED,
        TIMEOUT, FINISHED, CANCELED, EXPIRED
    }
}
