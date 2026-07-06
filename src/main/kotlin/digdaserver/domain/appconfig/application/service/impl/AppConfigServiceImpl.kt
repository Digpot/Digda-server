package digdaserver.domain.appconfig.application.service.impl

import digdaserver.domain.appconfig.application.service.AppConfigService
import digdaserver.domain.appconfig.domain.entity.AppConfig
import digdaserver.domain.appconfig.domain.repository.AppConfigRepository
import digdaserver.domain.appconfig.presentation.dto.req.UpdateAppConfigRequest
import digdaserver.domain.appconfig.presentation.dto.res.AppConfigResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AppConfigServiceImpl(
    private val appConfigRepository: AppConfigRepository
) : AppConfigService {

    override fun get(): AppConfigResponse {
        val config = appConfigRepository.findFirstByOrderByIdAsc()
        return config?.let(AppConfigResponse::from) ?: AppConfigResponse.default
    }

    @Transactional
    override fun update(request: UpdateAppConfigRequest): AppConfigResponse {
        val config = appConfigRepository.findFirstByOrderByIdAsc() ?: AppConfig()
        config.update(
            noticeEnabled = request.noticeEnabled,
            noticeMessage = request.noticeMessage.trim(),
            feedbackEnabled = request.feedbackEnabled,
            feedbackUrl = request.feedbackUrl.trim()
        )
        return AppConfigResponse.from(appConfigRepository.save(config))
    }
}
