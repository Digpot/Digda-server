package digdaserver.domain.appconfig.application.service

import digdaserver.domain.appconfig.presentation.dto.req.UpdateAppConfigRequest
import digdaserver.domain.appconfig.presentation.dto.res.AppConfigResponse

interface AppConfigService {

    /** 현재 앱 운영 설정(없으면 기본값). */
    fun get(): AppConfigResponse

    /** 앱 운영 설정 갱신(단일 행 upsert). 어드민 전용. */
    fun update(request: UpdateAppConfigRequest): AppConfigResponse
}
