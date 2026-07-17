package digdaserver.domain.minigame.presentation

import digdaserver.domain.catchmind.application.CatchmindGameManager
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.molebattle.application.MoleBattleGameManager
import digdaserver.domain.omok.application.OmokGameManager
import digdaserver.domain.tapbattle.application.TapBattleGameManager
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 게임하기 화면용 통합 조회 — 이 그룹에서 내가 초대받아 대기 중인 게임과
 * 참여 중인(재입장 가능한) 게임을 한 번에 내려준다. 알림을 놓쳐도 게임 메뉴에서
 * 초대를 받을 수 있게 하는 진입점.
 */
@RestController
@RequestMapping("/games")
@Tag(name = "GameInvites", description = "미니게임 초대/진행 중 목록 통합 조회")
class GameInviteController(
    private val omokGameManager: OmokGameManager,
    private val catchmindGameManager: CatchmindGameManager,
    private val tapBattleGameManager: TapBattleGameManager,
    private val moleBattleGameManager: MoleBattleGameManager,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    data class GameInviteItem(
        val gameType: String,
        val gameId: Long,
        val groupRoomId: Long,
        /** 초대장: 초대한 사람 이름. 진행 중: 상대/방 설명. */
        val title: String,
        val createdAtEpochMs: Long,
        /** CATCHMIND 진행 중 카드용 참가 인원(그 외 2). */
        val playerCount: Int
    )

    data class GameInvitesResponse(
        val invites: List<GameInviteItem>,
        val active: List<GameInviteItem>
    )

    @Operation(summary = "초대·진행 중 게임 목록", description = "이 그룹에서 내가 초대받은 대기 게임 + 참여 중 게임(재입장)을 조회합니다.")
    @GetMapping("/invites")
    fun invites(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long
    ): ResponseEntity<GameInvitesResponse> {
        log.info("api=GET /games/invites, userId={}, groupRoomId={}", userId, groupRoomId)
        val uid = UUID.fromString(userId)
        validateGroupMember(groupRoomId, uid)

        val invites = buildList {
            omokGameManager.pendingInvitesFor(uid, groupRoomId).forEach {
                add(
                    GameInviteItem(
                        gameType = "OMOK",
                        gameId = it.id,
                        groupRoomId = it.groupRoomId,
                        title = it.inviterName,
                        createdAtEpochMs = it.createdAt.toEpochMilli(),
                        playerCount = 2
                    )
                )
            }
            catchmindGameManager.pendingInvitesFor(uid, groupRoomId).forEach {
                add(
                    GameInviteItem(
                        gameType = "CATCHMIND",
                        gameId = it.id,
                        groupRoomId = it.groupRoomId,
                        title = it.hostName,
                        createdAtEpochMs = it.createdAt.toEpochMilli(),
                        playerCount = it.joinedPlayers().size
                    )
                )
            }
            tapBattleGameManager.pendingInvitesFor(uid, groupRoomId).forEach {
                add(
                    GameInviteItem(
                        gameType = "TAP_BATTLE",
                        gameId = it.id,
                        groupRoomId = it.groupRoomId,
                        title = it.inviterName,
                        createdAtEpochMs = it.createdAt.toEpochMilli(),
                        playerCount = 2
                    )
                )
            }
            moleBattleGameManager.pendingInvitesFor(uid, groupRoomId).forEach {
                add(
                    GameInviteItem(
                        gameType = "MOLE_BATTLE",
                        gameId = it.id,
                        groupRoomId = it.groupRoomId,
                        title = it.inviterName,
                        createdAtEpochMs = it.createdAt.toEpochMilli(),
                        playerCount = 2
                    )
                )
            }
        }.sortedByDescending { it.createdAtEpochMs }

        val active = buildList {
            omokGameManager.activeGamesFor(uid, groupRoomId).forEach {
                add(
                    GameInviteItem(
                        gameType = "OMOK",
                        gameId = it.id,
                        groupRoomId = it.groupRoomId,
                        title = if (it.inviterId == uid) it.inviteeName else it.inviterName,
                        createdAtEpochMs = it.createdAt.toEpochMilli(),
                        playerCount = 2
                    )
                )
            }
            catchmindGameManager.activeGamesFor(uid, groupRoomId).forEach {
                add(
                    GameInviteItem(
                        gameType = "CATCHMIND",
                        gameId = it.id,
                        groupRoomId = it.groupRoomId,
                        title = it.hostName,
                        createdAtEpochMs = it.createdAt.toEpochMilli(),
                        playerCount = it.joinedPlayers().size
                    )
                )
            }
            tapBattleGameManager.activeGamesFor(uid, groupRoomId).forEach {
                add(
                    GameInviteItem(
                        gameType = "TAP_BATTLE",
                        gameId = it.id,
                        groupRoomId = it.groupRoomId,
                        title = if (it.inviterId == uid) it.inviteeName else it.inviterName,
                        createdAtEpochMs = it.createdAt.toEpochMilli(),
                        playerCount = 2
                    )
                )
            }
            moleBattleGameManager.activeGamesFor(uid, groupRoomId).forEach {
                add(
                    GameInviteItem(
                        gameType = "MOLE_BATTLE",
                        gameId = it.id,
                        groupRoomId = it.groupRoomId,
                        title = if (it.inviterId == uid) it.inviteeName else it.inviterName,
                        createdAtEpochMs = it.createdAt.toEpochMilli(),
                        playerCount = 2
                    )
                )
            }
        }.sortedByDescending { it.createdAtEpochMs }

        return ResponseEntity.ok(GameInvitesResponse(invites = invites, active = active))
    }

    private fun validateGroupMember(groupRoomId: Long, userId: UUID) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        if (groupRoom.deletedAt != null) {
            throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)
        }
        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }
    }
}
