package digdaserver.global.infra.fcm.presentation.application.impl

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
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

        val distinctTokens = tokens.distinct()
        val invalidTokens = mutableListOf<String>()
        var successCount = 0
        var failureCount = 0

        distinctTokens.chunked(500).forEach { chunk ->
            val message = MulticastMessage.builder()
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                .putAllData(data)
                .addAllTokens(chunk)
                .build()

            try {
                val response = firebaseMessaging.sendEachForMulticast(message)
                successCount += response.successCount
                failureCount += response.failureCount

                response.responses.forEachIndexed { idx, resp ->
                    if (!resp.isSuccessful) {
                        val errorCode = resp.exception?.messagingErrorCode
                        if (errorCode == MessagingErrorCode.UNREGISTERED ||
                            errorCode == MessagingErrorCode.INVALID_ARGUMENT
                        ) {
                            invalidTokens.add(chunk[idx])
                        } else {
                            log.warn("FCM send failed for token ({}): {}", errorCode, resp.exception?.message)
                        }
                    }
                }
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
}
