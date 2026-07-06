package digdaserver.domain.block.domain.repository

import digdaserver.domain.block.domain.entity.ContentHide
import digdaserver.domain.block.domain.entity.HideTargetType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ContentHideRepository : JpaRepository<ContentHide, Long> {

    fun existsByUserIdAndTargetTypeAndTargetId(
        userId: UUID,
        targetType: HideTargetType,
        targetId: Long
    ): Boolean

    fun findByUserIdAndTargetTypeAndTargetId(
        userId: UUID,
        targetType: HideTargetType,
        targetId: Long
    ): ContentHide?

    /** 조회 필터용 — 특정 사용자가 특정 종류에서 숨긴 콘텐츠 목록(id→reason 매핑에 사용). */
    fun findAllByUserIdAndTargetType(userId: UUID, targetType: HideTargetType): List<ContentHide>
}
