package digdaserver.global.infra.fcm.presentation.application

import digdaserver.global.infra.fcm.dto.FcmSendResult

interface FcmService {

    fun sendToTokens(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): FcmSendResult
}
