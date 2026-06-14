package digdaserver.admin.report.application.service.impl

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.report.application.service.AdminReportService
import digdaserver.admin.report.presentation.dto.res.AdminReportResponse
import digdaserver.domain.comment.domain.repository.CommentRepository
import digdaserver.domain.diary.domain.repository.DiaryRepository
import digdaserver.domain.report.domain.entity.Report
import digdaserver.domain.report.domain.entity.ReportStatus
import digdaserver.domain.report.domain.entity.ReportTargetType
import digdaserver.domain.report.domain.repository.ReportRepository
import digdaserver.domain.schedule.domain.repository.ScheduleRepository
import digdaserver.domain.user.domain.entity.User
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AdminReportServiceImpl(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val diaryRepository: DiaryRepository,
    private val scheduleRepository: ScheduleRepository,
    private val commentRepository: CommentRepository
) : AdminReportService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun search(
        status: ReportStatus?,
        targetType: ReportTargetType?,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminReportResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = reportRepository.searchForAdmin(status, targetType, pageable)
        return AdminPageResponse.of(result) { report ->
            val reported = resolveReportedUser(report)
            AdminReportResponse.from(report, reported?.id?.toString(), reported?.displayedName())
        }
    }

    @Transactional
    override fun updateStatus(reportId: Long, status: ReportStatus): AdminReportResponse {
        val report = reportRepository.findById(reportId)
            .orElseThrow { DigdaException(ErrorCode.RESOURCE_NOT_FOUND) }
        report.markReviewed(status)
        val reported = resolveReportedUser(report)
        return AdminReportResponse.from(report, reported?.id?.toString(), reported?.displayedName())
    }

    /**
     * 신고 대상(targetId)에서 피신고자(작성자)를 해석한다.
     * - USER: targetId 가 UUID → 그 사용자 본인
     * - DIARY/SCHEDULE/COMMENT: targetId 가 콘텐츠 PK → 작성자(createdBy)
     * 콘텐츠가 이미 삭제됐거나 id 파싱이 실패하면 null (어드민 화면에서 '-' 표기).
     */
    private fun resolveReportedUser(report: Report): User? {
        return try {
            when (report.targetType) {
                ReportTargetType.USER ->
                    userRepository.findById(UUID.fromString(report.targetId)).orElse(null)
                ReportTargetType.DIARY ->
                    report.targetId.toLongOrNull()
                        ?.let { diaryRepository.findById(it).orElse(null)?.createdBy }
                ReportTargetType.SCHEDULE ->
                    report.targetId.toLongOrNull()
                        ?.let { scheduleRepository.findById(it).orElse(null)?.createdBy }
                ReportTargetType.COMMENT ->
                    report.targetId.toLongOrNull()
                        ?.let { commentRepository.findById(it).orElse(null)?.createdBy }
            }
        } catch (e: Exception) {
            log.warn(
                "action=피신고자 해석 실패, reportId={}, targetType={}, targetId={}, error={}",
                report.id,
                report.targetType,
                report.targetId,
                e.message
            )
            null
        }
    }
}
