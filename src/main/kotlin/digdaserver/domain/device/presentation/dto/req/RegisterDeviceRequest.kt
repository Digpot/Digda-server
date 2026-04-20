package digdaserver.domain.device.presentation.dto.req

data class RegisterDeviceRequest(
    val token: String,
    val platform: String
)
