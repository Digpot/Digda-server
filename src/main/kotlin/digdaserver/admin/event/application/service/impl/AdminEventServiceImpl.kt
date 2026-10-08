package digdaserver.admin.event.application.service.impl

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.event.application.service.AdminEventService
import digdaserver.admin.event.presentation.dto.req.GrantCoinRequest
import digdaserver.admin.event.presentation.dto.req.UpdateExpEventRequest
import digdaserver.admin.event.presentation.dto.res.CoinGrantResponse
import digdaserver.domain.announcement.application.service.AnnouncementService
import digdaserver.domain.announcement.domain.entity.AnnouncementTarget
import digdaserver.domain.character.domain.entity.GroupCharacter
import digdaserver.domain.character.domain.repository.GroupCharacterRepository
import digdaserver.domain.event.domain.entity.CoinGrant
import digdaserver.domain.event.domain.entity.ExpEvent
import digdaserver.domain.event.domain.repository.CoinGrantRepository
import digdaserver.domain.event.domain.repository.ExpEventRepository
import digdaserver.domain.event.presentation.dto.res.ExpEventResponse
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.infra.logging.LogUserContext
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AdminEventServiceImpl(
    private val expEventRepository: ExpEventRepository,
    private val coinGrantRepository: CoinGrantRepository,
    private val groupCharacterRepository: GroupCharacterRepository,
    private val announcementService: AnnouncementService
) : AdminEventService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getExpEvent(): ExpEventResponse {
        val event = expEventRepository.findFirstByOrderByIdAsc()
        return event?.let { ExpEventResponse.from(it, LocalDateTime.now()) }
            ?: ExpEventResponse.default
    }

    @Transactional
    override fun updateExpEvent(request: UpdateExpEventRequest): ExpEventResponse {
        val start = request.startAt
        val end = request.endAt
        if (start != null && end != null && !end.isAfter(start)) {
            throw DigdaException(ErrorCode.INVALID_PARAMETER, "종료 시각은 시작 시각보다 뒤여야 합니다")
        }
        // 켜는 순간 배수가 1.0 이면 아무 일도 일어나지 않아 "켰는데 왜 그대로냐" 문의로 돌아온다.
        if (request.enabled && request.multiplier <= 1.0) {
            throw DigdaException(ErrorCode.INVALID_PARAMETER, "이벤트를 켜려면 배수가 1.0보다 커야 합니다")
        }

        val event = expEventRepository.findFirstByOrderByIdAsc() ?: ExpEvent()
        event.update(
            enabled = request.enabled,
            title = request.title.trim(),
            multiplier = request.multiplier,
            startAt = start,
            endAt = end
        )
        val saved = expEventRepository.save(event)

        log.info(
            "action=admin_exp_event_update, adminId={}, enabled={}, multiplier={}, startAt={}, endAt={}",
            LogUserContext.currentUserId(),
            saved.enabled,
            saved.multiplier,
            saved.startAt,
            saved.endAt
        )
        return ExpEventResponse.from(saved, LocalDateTime.now())
    }

    @Transactional
    override fun grantCoinToAll(request: GrantCoinRequest): CoinGrantResponse {
        // DTO Validation 으로 1..100000 보장. 컨트롤러 우회 대비 방어.
        if (request.amount !in 1..MAX_GRANT_AMOUNT) {
            throw DigdaException(ErrorCode.INVALID_PARAMETER, "지급 코인은 1 ~ $MAX_GRANT_AMOUNT 사이여야 합니다")
        }
        if (request.notify && (request.notificationTitle.isNullOrBlank() || request.notificationBody.isNullOrBlank())) {
            throw DigdaException(ErrorCode.INVALID_PARAMETER, "공지를 보내려면 제목과 본문이 필요합니다")
        }

        // 모찌 행은 캐릭터 화면 첫 진입 때 만들어진다. 아직 안 들어가 본 그룹도 이벤트에서
        // 빠지지 않도록 지급 직전에 빈 행을 채워둔다. (기본 아이템 지급/장착은 캐릭터 화면
        // 진입 시 CharacterGearInitializer 가 idempotent 하게 보정하므로 여기서는 생략)
        val missing = groupCharacterRepository.findActiveGroupRoomsWithoutCharacter()
        if (missing.isNotEmpty()) {
            groupCharacterRepository.saveAll(missing.map { GroupCharacter(groupRoom = it) })
        }

        val targetCount = groupCharacterRepository.addCoinToActiveGroups(
            amount = request.amount,
            now = LocalDateTime.now()
        )

        val grantedBy = LogUserContext.currentUserId()
        val saved = coinGrantRepository.save(
            CoinGrant(
                amount = request.amount,
                reason = request.reason.trim(),
                targetCount = targetCount,
                notified = request.notify,
                grantedBy = grantedBy
            )
        )

        log.info(
            "action=admin_coin_grant, adminId={}, amount={}, targetCount={}, createdCharacters={}, " +
                "notify={}, reason={}",
            grantedBy,
            request.amount,
            targetCount,
            missing.size,
            request.notify,
            saved.reason
        )

        // 공지는 지급이 끝난 뒤 같은 트랜잭션에서 보낸다 — 발송이 실패하면 지급까지 함께
        // 롤백되어 "선물은 안 왔는데 공지만 온" 상태가 남지 않는다(어드민이 그대로 재실행).
        if (request.notify) {
            announcementService.send(
                target = AnnouncementTarget.ALL,
                targetUserIds = null,
                title = request.notificationTitle!!.trim(),
                body = request.notificationBody!!.trim()
            )
        }

        return CoinGrantResponse.from(saved)
    }

    override fun searchCoinGrants(page: Int, size: Int): AdminPageResponse<CoinGrantResponse> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val result = coinGrantRepository.findAllByOrderByIdDesc(pageable)
        return AdminPageResponse.of(result, CoinGrantResponse::from)
    }

    companion object {
        /** 1회 지급 상한 — 0 하나 더 붙는 실수로 경제가 무너지는 것을 막는다. */
        private const val MAX_GRANT_AMOUNT = 100_000
    }
}
