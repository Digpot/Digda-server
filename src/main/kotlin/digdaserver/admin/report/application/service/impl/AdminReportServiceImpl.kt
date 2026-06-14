package digdaserver.admin.report.application.service.impl

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.report.application.service.AdminReportService
import digdaserver.admin.report.presentation.dto.res.AdminReportResponse
import digdaserver.admin.report.presentation.dto.res.AdminReportTargetContentResponse
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
            AdminReportResponse.from(
                report,
                reported?.id?.toString(),
                reported?.displayedName(),
                resolveTargetContent(report)
            )
        }
    }

    @Transactional
    override fun updateStatus(reportId: Long, status: ReportStatus): AdminReportResponse {
        val report = reportRepository.findById(reportId)
            .orElseThrow { DigdaException(ErrorCode.RESOURCE_NOT_FOUND) }
        report.markReviewed(status)
        val reported = resolveReportedUser(report)
        return AdminReportResponse.from(
            report,
            reported?.id?.toString(),
            reported?.displayedName(),
            resolveTargetContent(report)
        )
    }

    /**
     * 신고 대상(targetId)의 원본 콘텐츠를 어드민 검토용 스냅샷으로 만든다.
     * - DIARY:    제목 + 본문 + 사진 URL 들
     * - SCHEDULE: 제목 + 기간/시간 요약
     * - COMMENT:  댓글 내용
     * - USER:     원본 콘텐츠 없음(available=false)
     * 콘텐츠가 삭제됐거나 파싱 실패하면 available=false.
     */
    private fun resolveTargetContent(report: Report): AdminReportTargetContentResponse {
        return try {
            when (report.targetType) {
                ReportTargetType.USER -> AdminReportTargetContentResponse.unavailable()

                ReportTargetType.DIARY ->
                    report.targetId.toLongOrNull()
                        ?.let { diaryRepository.findById(it).orElse(null) }
                        ?.let { diary ->
                            AdminReportTargetContentResponse(
                                available = true,
                                title = diary.title,
                                text = diary.content,
                                images = diary.images.map { it.url },
                                authorName = diary.createdBy.displayedName(),
                                createdAt = diary.createdAt
                            )
                        } ?: AdminReportTargetContentResponse.unavailable()

                ReportTargetType.SCHEDULE ->
                    report.targetId.toLongOrNull()
                        ?.let { scheduleRepository.findById(it).orElse(null) }
                        ?.let { schedule ->
                            val period = if (schedule.startDate == schedule.endDate) {
                                schedule.startDate.toString()
                            } else {
                                "${schedule.startDate} ~ ${schedule.endDate}"
                            }
                            val time = when {
                                schedule.allDay -> "종일"
                                schedule.startTime != null ->
                                    "${schedule.startTime}" +
                                        (schedule.endTime?.let { " ~ $it" } ?: "")
                                else -> ""
                            }
                            AdminReportTargetContentResponse(
                                available = true,
                                title = schedule.title,
                                text = listOf(period, time).filter { it.isNotBlank() }
                                    .joinToString(" · "),
                                authorName = schedule.createdBy.displayedName(),
                                createdAt = schedule.createdAt
                            )
                        } ?: AdminReportTargetContentResponse.unavailable()

                ReportTargetType.COMMENT ->
                    report.targetId.toLongOrNull()
                        ?.let { commentRepository.findById(it).orElse(null) }
                        ?.let { comment ->
                            AdminReportTargetContentResponse(
                                available = true,
                                text = comment.text,
                                authorName = comment.createdBy.displayedName(),
                                createdAt = comment.createdAt
                            )
                        } ?: AdminReportTargetContentResponse.unavailable()
            }
        } catch (e: Exception) {
            log.warn(
                "action=신고 원본 콘텐츠 해석 실패, reportId={}, targetType={}, targetId={}, error={}",
                report.id,
                report.targetType,
                report.targetId,
                e.message
            )
            AdminReportTargetContentResponse.unavailable()
        }
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
