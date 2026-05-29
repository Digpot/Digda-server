package digdaserver.admin.character.presentation.dto.res

import digdaserver.domain.character.application.level.CharacterLevelTable
import digdaserver.domain.character.domain.entity.CharacterStage
import digdaserver.domain.character.domain.entity.GroupCharacter
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 어드민용 그룹 모찌 정보. 그룹방 컨텍스트(이름·삭제여부)와 캐릭터 본문(레벨·exp·코인·디코)
 * 을 함께 노출해 어드민이 "어느 그룹의 모찌인지" 파악하기 쉽도록 했다.
 *
 * 응답에 다음 레벨까지의 임계치([expForNextLevel])를 함께 내려 어드민 진행률 바를 그릴 수 있다.
 */
@Schema(description = "어드민용 그룹 모찌 정보")
data class AdminCharacterResponse(

    @Schema(description = "캐릭터 PK")
    val characterId: Long,

    @Schema(description = "그룹방 ID")
    val groupRoomId: Long,

    @Schema(description = "그룹방 이름")
    val groupRoomName: String,

    @Schema(description = "방장 이름")
    val ownerName: String,

    @Schema(description = "그룹방 삭제 시각 (null = 활성)")
    val groupRoomDeletedAt: LocalDateTime?,

    @Schema(description = "현재 진화 단계")
    val stage: CharacterStage,

    @Schema(description = "현재 진화 단계 표시명")
    val stageDisplayName: String,

    @Schema(description = "현재 레벨")
    val level: Int,

    @Schema(description = "현재 누적 EXP (현재 레벨 내)")
    val exp: Int,

    @Schema(description = "다음 레벨까지 필요한 EXP. maxLevel 도달 시 0")
    val expForNextLevel: Int,

    @Schema(description = "보유 코인")
    val coin: Int,

    @Schema(description = "MAX 레벨 도달 여부")
    val maxLevelReached: Boolean,

    @Schema(description = "디코 해금 여부")
    val dikoUnlocked: Boolean,

    @Schema(description = "생성 시각")
    val createdAt: LocalDateTime,

    @Schema(description = "수정 시각")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(character: GroupCharacter): AdminCharacterResponse {
            val atMax = character.level >= CharacterLevelTable.MAX_LEVEL
            return AdminCharacterResponse(
                characterId = character.id,
                groupRoomId = character.groupRoom.id,
                groupRoomName = character.groupRoom.name,
                ownerName = character.groupRoom.owner.name,
                groupRoomDeletedAt = character.groupRoom.deletedAt,
                stage = character.stage,
                stageDisplayName = character.stage.displayName,
                level = character.level,
                exp = character.exp,
                expForNextLevel = if (atMax) 0 else CharacterLevelTable.expForNextLevel(character.level),
                coin = character.coin,
                maxLevelReached = atMax,
                dikoUnlocked = character.dikoUnlocked,
                createdAt = character.createdAt,
                updatedAt = character.updatedAt
            )
        }
    }
}
