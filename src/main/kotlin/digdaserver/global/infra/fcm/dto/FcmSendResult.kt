package digdaserver.global.infra.fcm.dto

data class FcmSendResult(
    val successCount: Int,
    val failureCount: Int,
    val invalidTokens: List<String>
) {
    companion object {
        fun empty(): FcmSendResult = FcmSendResult(0, 0, emptyList())
    }
}
