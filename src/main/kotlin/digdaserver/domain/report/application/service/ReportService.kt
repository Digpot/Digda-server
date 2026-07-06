package digdaserver.domain.report.application.service

import digdaserver.domain.report.presentation.dto.req.CreateReportRequest
import digdaserver.domain.report.presentation.dto.res.ReportResponse
import java.util.UUID

interface ReportService {

    /**
     * 신고 접수. 어드민 검토용 기록을 남기고, 콘텐츠형(일기/댓글/일정) 신고는 신고자 본인에게서
     * 자동 숨김(REPORTED)까지 처리한다. 같은 대상 중복 신고는 멱등 처리.
     */
    fun createReport(reporterId: UUID, request: CreateReportRequest): ReportResponse
}
