package digdaserver.admin.announcement.presentation.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관리자 공지 발송 응답")
data class SendAnnouncementResponse(

    @Schema(description = "실제로 발송된 수신자 수")
    val recipientCount: Int
)
