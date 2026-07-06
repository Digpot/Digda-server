package digdaserver.domain.report.application.service.impl

import digdaserver.domain.block.application.service.BlockService
import digdaserver.domain.block.domain.entity.HideReason
import digdaserver.domain.block.domain.entity.HideTargetType
import digdaserver.domain.log.application.service.UserActionLogService
import digdaserver.domain.log.domain.entity.UserAction
import digdaserver.domain.report.application.service.ReportService
import digdaserver.domain.report.domain.entity.Report
import digdaserver.domain.report.domain.entity.ReportStatus
import digdaserver.domain.report.domain.entity.ReportTargetType
import digdaserver.domain.report.domain.repository.ReportRepository
import digdaserver.domain.report.presentation.dto.req.CreateReportRequest
import digdaserver.domain.report.presentation.dto.res.ReportResponse
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ReportServiceImpl(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val blockService: BlockService,
    private val userActionLogService: UserActionLogService
) : ReportService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun createReport(reporterId: UUID, request: CreateReportRequest): ReportResponse {
        val reporter = userRepository.findById(reporterId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        // targetId 형식 검증 — 콘텐츠형은 Long, USER 는 UUID 여야 한다.
        val contentTargetId: Long? = when (request.targetType) {
            ReportTargetType.USER -> {
                val targetUuid = runCatching { UUID.fromString(request.targetId) }.getOrNull()
                    ?: throw DigdaException(ErrorCode.REPORT_INVALID_TARGET)
                if (targetUuid == reporterId) throw DigdaException(ErrorCode.CANNOT_REPORT_SELF)
                null
            }
            else -> request.targetId.toLongOrNull()
                ?: throw DigdaException(ErrorCode.REPORT_INVALID_TARGET)
        }

        // 중복 신고는 멱등 — 새 기록을 만들지 않되, 자동 숨김은 보장한다.
        val alreadyReported = reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
            reporterId,
            request.targetType,
            request.targetId
        )

        val report = if (alreadyReported) {
            log.info(
                "action=신고 멱등 스킵(이미 신고함), reporterId={}, type={}, targetId={}",
                reporterId,
                request.targetType,
                request.targetId
            )
            null
        } else {
            reportRepository.save(
                Report(
                    reporter = reporter,
                    targetType = request.targetType,
                    targetId = request.targetId,
                    groupRoomId = request.groupRoomId,
                    reason = request.reason,
                    detail = request.detail?.takeIf { it.isNotBlank() }
                )
            )
        }

        // 콘텐츠형 신고는 신고자 본인에게서 자동 숨김(REPORTED). USER 신고는 차단을 별도 액션으로 둔다.
        contentTargetId?.let { id ->
            val hideType = when (request.targetType) {
                ReportTargetType.DIARY -> HideTargetType.DIARY
                ReportTargetType.COMMENT -> HideTargetType.COMMENT
                ReportTargetType.SCHEDULE -> HideTargetType.SCHEDULE
                ReportTargetType.USER -> null
            }
            hideType?.let { blockService.hideContent(reporterId, it, id, HideReason.REPORTED) }
        }

        userActionLogService.record(
            actorId = reporterId,
            action = UserAction.REPORT,
            targetType = request.targetType.name,
            targetId = request.targetId,
            detail = "reason=${request.reason}, groupRoomId=${request.groupRoomId}"
        )
        log.info(
            "action=신고 접수, reporterId={}, type={}, targetId={}, reason={}",
            reporterId,
            request.targetType,
            request.targetId,
            request.reason
        )

        // 멱등 스킵이면 기존 기록 응답을 만들 필요까진 없어 합성 응답을 돌려준다(상태는 PENDING 가정).
        return report?.let { ReportResponse.from(it) } ?: ReportResponse(
            id = 0L,
            targetType = request.targetType,
            targetId = request.targetId,
            reason = request.reason,
            status = ReportStatus.PENDING,
            createdAt = LocalDateTime.now()
        )
    }
}
