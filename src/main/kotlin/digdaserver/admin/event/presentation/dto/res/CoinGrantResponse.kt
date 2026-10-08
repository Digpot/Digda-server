package digdaserver.admin.event.presentation.dto.res

import digdaserver.domain.event.domain.entity.CoinGrant
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "코인 일괄 지급 이력 1건")
data class CoinGrantResponse(

    @Schema(description = "지급 이력 ID")
    val coinGrantId: Long,

    @Schema(description = "그룹(모찌) 1개당 지급한 코인")
    val amount: Int,

    @Schema(description = "운영 메모")
    val reason: String,

    @Schema(description = "코인이 들어간 그룹 캐릭터 수")
    val targetCount: Int,

    @Schema(description = "전체 공지 푸시를 함께 보냈는지")
    val notified: Boolean,

    @Schema(description = "실행한 어드민")
    val grantedBy: String,

    @Schema(description = "지급 시각")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(grant: CoinGrant): CoinGrantResponse = CoinGrantResponse(
            coinGrantId = grant.id,
            amount = grant.amount,
            reason = grant.reason,
            targetCount = grant.targetCount,
            notified = grant.notified,
            grantedBy = grant.grantedBy,
            createdAt = grant.createdAt
        )
    }
}
