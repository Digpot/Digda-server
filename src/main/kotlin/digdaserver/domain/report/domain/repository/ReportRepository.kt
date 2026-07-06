package digdaserver.domain.report.domain.repository

import digdaserver.domain.report.domain.entity.Report
import digdaserver.domain.report.domain.entity.ReportStatus
import digdaserver.domain.report.domain.entity.ReportTargetType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ReportRepository : JpaRepository<Report, Long> {

    /** 같은 신고자가 같은 대상을 중복 신고하지 못하도록 확인. */
    fun existsByReporterIdAndTargetTypeAndTargetId(
        reporterId: UUID,
        targetType: ReportTargetType,
        targetId: String
    ): Boolean

    /** 어드민 목록 — 상태/타입 선택 필터(둘 다 null 이면 전체). 최신순은 Pageable 로 지정. */
    @Query(
        """
        SELECT r FROM Report r
        WHERE (:status IS NULL OR r.status = :status)
          AND (:targetType IS NULL OR r.targetType = :targetType)
        """
    )
    fun searchForAdmin(
        @Param("status") status: ReportStatus?,
        @Param("targetType") targetType: ReportTargetType?,
        pageable: Pageable
    ): Page<Report>

    fun countByStatus(status: ReportStatus): Long
}
