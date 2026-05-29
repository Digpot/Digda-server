package digdaserver.domain.character.presentation.controller

import digdaserver.domain.character.application.service.CharacterService
import digdaserver.domain.character.presentation.dto.req.AddExpRequest
import digdaserver.domain.character.presentation.dto.req.MasterGameRewardRequest
import digdaserver.domain.character.presentation.dto.res.AddExpResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStageTreeResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStateResponse
import digdaserver.domain.character.presentation.dto.res.MasterGameRewardResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/character")
@Tag(name = "Character", description = "캐릭터(모찌) 키우기 API — 그룹 1개당 1마리 공유")
class CharacterController(
    private val characterService: CharacterService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(
        summary = "그룹 캐릭터 상태 조회",
        description = "해당 그룹의 모찌. 첫 진입 시 자동 생성됩니다. 호출자가 그룹 멤버여야 함."
    )
    @GetMapping
    fun getGroupCharacter(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long
    ): ResponseEntity<CharacterStateResponse> {
        log.info("api=GET /character, userId={}, groupRoomId={}", userId, groupRoomId)
        return ResponseEntity.ok(
            characterService.getGroupCharacter(UUID.fromString(userId), groupRoomId)
        )
    }

    @Operation(
        summary = "경험치 가산",
        description = "그룹 캐릭터에 amount만큼 EXP를 더하고, 레벨업/진화는 서버가 계산해 응답에 포함합니다."
    )
    @PostMapping("/exp")
    fun gainExp(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @RequestBody request: AddExpRequest
    ): ResponseEntity<AddExpResponse> {
        log.info(
            "api=POST /character/exp, userId={}, groupRoomId={}, amount={}, source={}",
            userId,
            groupRoomId,
            request.amount,
            request.source
        )
        return ResponseEntity.ok(
            characterService.gainExp(
                userId = UUID.fromString(userId),
                groupRoomId = groupRoomId,
                amount = request.amount,
                coinDelta = 0,
                source = request.source
            )
        )
    }

    @Operation(summary = "진화 트리 조회", description = "전체 단계 + 그룹 캐릭터 도달 여부.")
    @GetMapping("/stages")
    fun getStageTree(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long
    ): ResponseEntity<CharacterStageTreeResponse> {
        log.info("api=GET /character/stages, userId={}, groupRoomId={}", userId, groupRoomId)
        return ResponseEntity.ok(
            characterService.getStageTree(UUID.fromString(userId), groupRoomId)
        )
    }

    @Operation(
        summary = "마스터 게임 보상",
        description = "마스터 단계 모찌의 챔피언 챌린지 점수를 제출하고 코인 보상을 받습니다."
    )
    @PostMapping("/master-game-reward")
    fun claimMasterGameReward(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @RequestBody request: MasterGameRewardRequest
    ): ResponseEntity<MasterGameRewardResponse> {
        log.info(
            "api=POST /character/master-game-reward, userId={}, groupRoomId={}, score={}",
            userId, groupRoomId, request.score
        )
        return ResponseEntity.ok(
            characterService.claimMasterGameReward(
                UUID.fromString(userId), groupRoomId, request.score
            )
        )
    }
}
