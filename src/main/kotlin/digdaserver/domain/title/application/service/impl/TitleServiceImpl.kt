package digdaserver.domain.title.application.service.impl

import digdaserver.domain.diary.domain.repository.DiaryRepository
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.title.application.service.TitleService
import digdaserver.domain.title.domain.entity.GroupEquippedTitle
import digdaserver.domain.title.domain.entity.UserTitle
import digdaserver.domain.title.domain.repository.GroupEquippedTitleRepository
import digdaserver.domain.title.domain.repository.UserTitleRepository
import digdaserver.domain.title.presentation.dto.req.ClaimTitleItem
import digdaserver.domain.title.presentation.dto.res.EquippedTitleResponse
import digdaserver.domain.title.presentation.dto.res.TitleResponse
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class TitleServiceImpl(
    private val userTitleRepository: UserTitleRepository,
    private val groupEquippedTitleRepository: GroupEquippedTitleRepository,
    private val diaryRepository: DiaryRepository,
    private val membershipRepository: MembershipRepository,
    private val groupRoomRepository: GroupRoomRepository
) : TitleService {

    private val codeFormat = Regex("^[a-z0-9_]{1,40}$")

    /** 작성 일기 수 누적 칭호 임계값 — 앱 카탈로그의 diary_* 코드와 짝이 맞아야 한다. */
    private val diaryMilestones = listOf(1, 10, 30, 50, 100)

    @Transactional
    override fun list(userId: UUID): List<TitleResponse> {
        grantDiaryCountTitles(userId)
        return userTitleRepository.findAllByUserIdOrderByEarnedAtDesc(userId)
            .map(TitleResponse::from)
    }

    @Transactional
    override fun claim(userId: UUID, items: List<ClaimTitleItem>): List<TitleResponse> {
        for (item in items) {
            val code = item.code.trim()
            if (!codeFormat.matches(code)) continue
            if (userTitleRepository.existsByUserIdAndCode(userId, code)) continue

            val groupId = item.groupRoomId
            var groupName: String? = null
            if (groupId != null) {
                // 멤버가 아닌 그룹으로의 적재는 무시(앱 버그/조작 방지).
                if (!membershipRepository.existsByGroupRoomIdAndUserId(groupId, userId)) continue
                groupName = groupRoomRepository.findById(groupId).orElse(null)?.name
            }
            userTitleRepository.save(
                UserTitle(
                    userId = userId,
                    code = code,
                    groupRoomId = groupId,
                    groupRoomName = groupName
                )
            )
        }
        return userTitleRepository.findAllByUserIdOrderByEarnedAtDesc(userId)
            .map(TitleResponse::from)
    }

    override fun equippedTitle(groupRoomId: Long): EquippedTitleResponse {
        return groupEquippedTitleRepository.findByGroupRoomId(groupRoomId)
            .map(EquippedTitleResponse::from)
            .orElse(EquippedTitleResponse.empty)
    }

    @Transactional
    override fun equipTitle(
        userId: UUID,
        groupRoomId: Long,
        code: String?
    ): EquippedTitleResponse {
        // 그룹 구성원만 그룹 모찌를 꾸밀 수 있다.
        if (!membershipRepository.existsByGroupRoomIdAndUserId(groupRoomId, userId)) {
            throw DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER)
        }
        val existing = groupEquippedTitleRepository.findByGroupRoomId(groupRoomId).orElse(null)

        // code 가 null/blank 면 해제.
        val target = code?.trim()
        if (target.isNullOrEmpty()) {
            if (existing != null) groupEquippedTitleRepository.delete(existing)
            return EquippedTitleResponse.empty
        }

        // 본인이 획득한 칭호만 장착 가능.
        if (!userTitleRepository.existsByUserIdAndCode(userId, target)) {
            throw DigdaException(ErrorCode.TITLE_NOT_OWNED)
        }

        val saved = if (existing != null) {
            existing.replaceWith(target, userId)
            existing
        } else {
            groupEquippedTitleRepository.save(
                GroupEquippedTitle(groupRoomId = groupRoomId, code = target, equippedBy = userId)
            )
        }
        return EquippedTitleResponse.from(saved)
    }

    /** 서버가 직접 셀 수 있는 전역 칭호(작성 일기 수)를 멱등 적재. 그룹 맥락 없음(null). */
    private fun grantDiaryCountTitles(userId: UUID) {
        val count = diaryRepository.countByCreatedById(userId)
        for (m in diaryMilestones) {
            if (count < m) break
            val code = "diary_$m"
            if (!userTitleRepository.existsByUserIdAndCode(userId, code)) {
                userTitleRepository.save(UserTitle(userId = userId, code = code))
            }
        }
    }
}
