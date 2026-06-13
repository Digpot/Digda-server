package digdaserver.domain.block.application.service

import digdaserver.domain.block.domain.entity.HideReason
import digdaserver.domain.block.domain.entity.HideTargetType
import digdaserver.domain.block.domain.repository.ContentHideRepository
import digdaserver.domain.block.domain.repository.UserBlockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 조회 시 "이 뷰어에게 숨겨야 하는가"를 한곳에서 판정한다.
 *
 * 핵심 원칙: 숨김 ≠ 삭제. 콘텐츠는 DB 와 집계(하루 1편 슬롯·시그니처 지도 색칠)에 그대로 남고,
 * 조회 시점에만 본문을 비우고 [VisibilityReason] 플래그를 내려보낸다. 그래서 차단/신고가
 * 그룹 공용 규칙(하루 1편)이나 지도 색칠을 깨뜨리지 않는다.
 *
 * 일기/댓글/일정 서비스는 목록을 만들 때 [forViewer] 를 한 번 호출해 컨텍스트를 받고,
 * 각 항목마다 `*HiddenReason(...)` 으로 숨김 사유 코드(없으면 null)를 구한다.
 */
@Service
@Transactional(readOnly = true)
class ContentVisibilityService(
    private val userBlockRepository: UserBlockRepository,
    private val contentHideRepository: ContentHideRepository
) {

    fun forViewer(viewerId: UUID): ViewerVisibility {
        val blockedUserIds = userBlockRepository.findBlockedIdsByBlockerId(viewerId).toHashSet()
        val hiddenDiary = contentHideRepository
            .findAllByUserIdAndTargetType(viewerId, HideTargetType.DIARY)
            .associate { it.targetId to it.reason }
        val hiddenComment = contentHideRepository
            .findAllByUserIdAndTargetType(viewerId, HideTargetType.COMMENT)
            .associate { it.targetId to it.reason }
        val hiddenSchedule = contentHideRepository
            .findAllByUserIdAndTargetType(viewerId, HideTargetType.SCHEDULE)
            .associate { it.targetId to it.reason }
        return ViewerVisibility(blockedUserIds, hiddenDiary, hiddenComment, hiddenSchedule)
    }
}

/** 숨김 사유 코드 — 앱이 플레이스홀더 문구를 분기하는 데 쓴다. */
object VisibilityReason {
    /** 작성자를 사용자 단위로 차단함. */
    const val BLOCKED_USER = "BLOCKED_USER"

    /** 신고하면서 자동 숨김됨. */
    const val REPORTED = "REPORTED"

    /** 이 게시물만 직접 숨김. */
    const val HIDDEN = "HIDDEN"
}

/**
 * 한 뷰어의 차단/숨김 스냅샷. 목록 1건 조회마다 재사용해 N+1 을 피한다.
 * 사용자 단위 차단이 개별 숨김보다 우선한다.
 */
class ViewerVisibility(
    val blockedUserIds: Set<UUID>,
    private val hiddenDiary: Map<Long, HideReason>,
    private val hiddenComment: Map<Long, HideReason>,
    private val hiddenSchedule: Map<Long, HideReason>
) {
    fun diaryHiddenReason(diaryId: Long, authorId: UUID): String? =
        resolve(authorId, hiddenDiary[diaryId])

    fun commentHiddenReason(commentId: Long, authorId: UUID): String? =
        resolve(authorId, hiddenComment[commentId])

    fun scheduleHiddenReason(scheduleId: Long, authorId: UUID): String? =
        resolve(authorId, hiddenSchedule[scheduleId])

    private fun resolve(authorId: UUID, itemHide: HideReason?): String? {
        if (authorId in blockedUserIds) return VisibilityReason.BLOCKED_USER
        return when (itemHide) {
            HideReason.REPORTED -> VisibilityReason.REPORTED
            HideReason.HIDDEN -> VisibilityReason.HIDDEN
            null -> null
        }
    }
}
