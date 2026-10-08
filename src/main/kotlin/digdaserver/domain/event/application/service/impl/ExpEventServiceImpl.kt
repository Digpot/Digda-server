package digdaserver.domain.event.application.service.impl

import digdaserver.domain.event.application.service.ExpEventService
import digdaserver.domain.event.domain.repository.ExpEventRepository
import digdaserver.domain.event.presentation.dto.res.ExpEventResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ExpEventServiceImpl(
    private val expEventRepository: ExpEventRepository
) : ExpEventService {

    override fun get(): ExpEventResponse {
        val event = expEventRepository.findFirstByOrderByIdAsc()
        return event?.let { ExpEventResponse.from(it, LocalDateTime.now()) }
            ?: ExpEventResponse.default
    }

    override fun boost(baseExp: Int, now: LocalDateTime): ExpEventService.ExpBoost {
        if (baseExp <= 0) return ExpEventService.ExpBoost.none(baseExp)
        val event = expEventRepository.findFirstByOrderByIdAsc()
            ?: return ExpEventService.ExpBoost.none(baseExp)
        return ExpEventService.ExpBoost(
            baseExp = baseExp,
            grantedExp = event.applyTo(baseExp, now),
            multiplier = event.multiplierAt(now)
        )
    }
}
