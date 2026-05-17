package digdaserver.global.infra.fcm.presentation.application.impl

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.google.firebase.messaging.SendResponse
import digdaserver.global.infra.fcm.dto.FcmSendResult
import digdaserver.global.infra.fcm.presentation.application.FcmService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FcmServiceImpl(
    private val firebaseMessaging: FirebaseMessaging
) : FcmService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendToTokens(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>
    ): FcmSendResult {
        if (tokens.isEmpty()) return FcmSendResult.empty()

        var successCount = 0
        var failureCount = 0
        val invalidTokens = mutableListOf<String>()

        tokens.distinct().chunked(MULTICAST_CHUNK_SIZE).forEach { chunk ->
            try {
                val response = firebaseMessaging.sendEachForMulticast(
                    buildMulticast(chunk, title, body, data)
                )
                successCount += response.successCount
                failureCount += response.failureCount
                invalidTokens += extractInvalidTokens(chunk, response.responses)
            } catch (e: FirebaseMessagingException) {
                failureCount += chunk.size
                log.error("FCM multicast send failed: {}", e.message, e)
            }
        }

        return FcmSendResult(
            successCount = successCount,
            failureCount = failureCount,
            invalidTokens = invalidTokens
        )
    }

    private fun buildMulticast(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>
    ): MulticastMessage = MulticastMessage.builder()
        .setNotification(
            Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build()
        )
        .setAndroidConfig(
            AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(
                    AndroidNotification.builder()
                        .setChannelId("digda_default")
                        .setPriority(AndroidNotification.Priority.HIGH)
                        .build()
                )
                .build()
        )
        .putAllData(data)
        .addAllTokens(tokens)
        .build()

    private fun extractInvalidTokens(
        chunk: List<String>,
        responses: List<SendResponse>
    ): List<String> {
        val invalid = mutableListOf<String>()
        responses.forEachIndexed { idx, resp ->
            if (resp.isSuccessful) return@forEachIndexed
            val errorCode = resp.exception?.messagingErrorCode
            if (errorCode in INVALID_TOKEN_ERRORS) {
                invalid += chunk[idx]
            } else {
                log.warn("FCM send failed for token ({}): {}", errorCode, resp.exception?.message)
            }
        }
        return invalid
    }

    companion object {
        private const val MULTICAST_CHUNK_SIZE = 500
        private val INVALID_TOKEN_ERRORS = setOf(
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT
        )
    }
}
