package digdaserver.admin.character.application.service.impl

import digdaserver.admin.character.application.service.AdminCharacterService
import digdaserver.admin.character.presentation.dto.req.AdminUpdateCharacterRequest
import digdaserver.admin.character.presentation.dto.res.AdminCharacterResponse
import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.domain.character.application.level.CharacterLevelTable
import digdaserver.domain.character.domain.repository.GroupCharacterRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminCharacterServiceImpl(
    private val groupCharacterRepository: GroupCharacterRepository
) : AdminCharacterService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun search(
        keyword: String?,
        includeDeletedGroups: Boolean,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminCharacterResponse> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)
        val pageable = PageRequest.of(
            safePage,
            safeSize,
            Sort.by(Sort.Direction.DESC, "updatedAt")
        )
        val result = groupCharacterRepository.searchForAdmin(
            keyword?.takeIf { it.isNotBlank() },
            includeDeletedGroups,
            pageable
        )
        return AdminPageResponse.of(result, AdminCharacterResponse::from)
    }

    override fun getDetail(groupRoomId: Long): AdminCharacterResponse {
        val character = groupCharacterRepository.findByGroupRoomId(groupRoomId)
            ?: throw DigdaException(ErrorCode.CHARACTER_NOT_FOUND)
        return AdminCharacterResponse.from(character)
    }

    @Transactional
    override fun update(
        groupRoomId: Long,
        request: AdminUpdateCharacterRequest
    ): AdminCharacterResponse {
        val character = groupCharacterRepository.findByGroupRoomId(groupRoomId)
            ?: throw DigdaException(ErrorCode.CHARACTER_NOT_FOUND)

        request.level?.let { newLevel ->
            // DTO Validation 으로 1..20 보장. 그래도 컨트롤러 우회/시리얼라이저 우회 대비 방어.
            val clamped = newLevel.coerceIn(1, CharacterLevelTable.MAX_LEVEL)
            character.adminSetLevel(clamped)
        }
        // EXP 는 레벨 적용 이후에 덮어쓴다 (adminSetLevel 이 exp 를 정합시키므로 순서 중요).
        request.exp?.let { newExp ->
            character.adminSetExp(newExp.coerceAtLeast(0))
        }
        request.coin?.let { newCoin ->
            val safe = newCoin.coerceAtLeast(0)
            character.adminSetCoin(safe)
        }
        request.dikoUnlocked?.let { unlocked ->
            character.adminSetDikoUnlocked(unlocked)
        }

        log.info(
            "action=admin_character_update, groupRoomId={}, level={}, exp={}, coin={}, " +
                "stage={}, dikoUnlocked={}",
            groupRoomId,
            character.level,
            character.exp,
            character.coin,
            character.stage,
            character.dikoUnlocked
        )
        return AdminCharacterResponse.from(character)
    }
}
