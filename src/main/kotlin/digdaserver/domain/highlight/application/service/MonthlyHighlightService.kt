package digdaserver.domain.highlight.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.highlight.domain.entity.MonthlyHighlight
import digdaserver.domain.highlight.domain.repository.MonthlyHighlightRepository
import digdaserver.domain.highlight.presentation.dto.res.HighlightDetailResponse
import digdaserver.domain.highlight.presentation.dto.res.HighlightListItem
import digdaserver.domain.highlight.presentation.dto.res.HighlightListResponse
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.util.UUID

/** 저장된 월간 하이라이트 조회. 계산은 하지 않고 스케줄러가 만든 결과만 읽는다. */
@Service
@Transactional(readOnly = true)
class MonthlyHighlightService(
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val highlightRepository: MonthlyHighlightRepository,
    private val objectMapper: ObjectMapper
) {

    fun list(userId: UUID, groupRoomId: Long): HighlightListResponse {
        ensureMember(userId, groupRoomId)
        val items = highlightRepository.findAllByGroupRoomIdOrderByTargetMonthDesc(groupRoomId).map {
            val ym = YearMonth.parse(it.targetMonth)
            HighlightListItem(
                month = it.targetMonth,
                year = ym.year,
                monthValue = ym.monthValue,
                title = titleOf(ym),
                totalDiaryCount = it.totalDiaryCount,
                coverImageUrl = it.coverImageUrl,
                generatedAt = it.generatedAt
            )
        }
        return HighlightListResponse(items)
    }

    fun get(userId: UUID, groupRoomId: Long, month: YearMonth): HighlightDetailResponse {
        val room = ensureMember(userId, groupRoomId)
        val entity = highlightRepository.findByGroupRoomIdAndTargetMonth(groupRoomId, month.toString())
            ?: throw DigdaException(ErrorCode.HIGHLIGHT_NOT_FOUND)
        val p = parse(entity)
        return HighlightDetailResponse(
            month = entity.targetMonth,
            year = month.year,
            monthValue = month.monthValue,
            title = titleOf(month),
            groupRoomName = room,
            generatedAt = entity.generatedAt,
            totalDiaryCount = p.totalDiaryCount,
            activeDays = p.activeDays,
            daysInMonth = p.daysInMonth,
            members = p.members,
            topWriters = p.topWriters,
            coverImageUrl = p.coverImageUrl,
            photos = p.photos,
            featuredDiaries = p.featuredDiaries,
            topLocation = p.topLocation,
            busiestWeekday = p.busiestWeekday,
            summary = p.summary,
            closingMessage = p.closingMessage
        )
    }

    private fun parse(entity: MonthlyHighlight): HighlightPayload =
        objectMapper.readValue(entity.payload, HighlightPayload::class.java)

    private fun titleOf(ym: YearMonth) = "${ym.year}년 ${ym.monthValue}월, 우리의 기록"

    /** 멤버십 검증 후 그룹 이름 반환. */
    private fun ensureMember(userId: UUID, groupRoomId: Long): String {
        val room = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        if (room.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)
        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }
        return room.name
    }
}
