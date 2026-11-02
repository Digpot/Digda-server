package digdaserver.domain.highlight.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import digdaserver.domain.comment.domain.entity.CommentTargetType
import digdaserver.domain.comment.domain.repository.CommentRepository
import digdaserver.domain.diary.domain.repository.DiaryLikeRepository
import digdaserver.domain.diary.domain.repository.DiaryReactionRepository
import digdaserver.domain.diary.domain.repository.DiaryRepository
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.highlight.domain.entity.MonthlyHighlight
import digdaserver.domain.highlight.domain.repository.MonthlyHighlightRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

/**
 * 그룹 1개의 특정 월 하이라이트를 집계해 저장한다. 멱등: 이미 있으면 아무것도 하지 않는다.
 * 한 트랜잭션에서 집계 → insert 하므로 중간 실패 시 행이 남지 않고, 다음 실행이 다시 시도한다.
 */
@Service
class MonthlyHighlightGenerator(
    private val groupRoomRepository: GroupRoomRepository,
    private val diaryRepository: DiaryRepository,
    private val membershipRepository: MembershipRepository,
    private val diaryLikeRepository: DiaryLikeRepository,
    private val diaryReactionRepository: DiaryReactionRepository,
    private val commentRepository: CommentRepository,
    private val highlightRepository: MonthlyHighlightRepository,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** @return 새로 만들었으면 true. 이미 있거나 대상 일기가 없으면 false. */
    @Transactional
    fun generate(groupRoomId: Long, month: YearMonth): Boolean {
        val targetMonth = month.toString()
        if (highlightRepository.existsByGroupRoomIdAndTargetMonth(groupRoomId, targetMonth)) return false

        val room = groupRoomRepository.findById(groupRoomId).orElse(null) ?: return false
        if (room.deletedAt != null) return false

        // diary.date 는 LocalDate(달력 날짜)라 타임존 변환 없이 월 경계 [1일, 말일] 로 정확히 거른다.
        val diaries = diaryRepository.findAllWithImagesByGroupRoomIdAndDateBetween(
            groupRoomId,
            month.atDay(1),
            month.atEndOfMonth()
        )
        if (diaries.isEmpty()) return false

        val ids = diaries.map { it.id }
        val likes = diaryLikeRepository.countByDiaryIdIn(ids).associate { (it[0] as Long) to (it[1] as Long).toInt() }
        val reactions = HashMap<Long, Int>()
        diaryReactionRepository.countGroupedByDiaryAndType(ids).forEach {
            reactions.merge(it[0] as Long, (it[2] as Long).toInt(), Int::plus)
        }
        val comments = commentRepository.countByTargetTypeAndTargetIdIn(CommentTargetType.DIARY, ids)
            .associate { (it[0] as Long) to (it[1] as Long).toInt() }

        val inputs = diaries.map { d ->
            MonthlyHighlightComposer.DiaryInput(
                id = d.id,
                title = d.title,
                content = d.content,
                date = d.date,
                authorId = d.createdBy.id.toString(),
                authorName = d.createdBy.displayedName(),
                authorImage = d.createdBy.profileImage,
                imageUrls = d.images.map { it.url },
                location = d.location,
                likeCount = likes[d.id] ?: 0,
                reactionCount = reactions[d.id] ?: 0,
                commentCount = comments[d.id] ?: 0
            )
        }
        val members = membershipRepository.findAllByGroupRoomId(groupRoomId).map {
            MonthlyHighlightComposer.MemberInput(it.user.id.toString(), it.user.displayedName(), it.user.profileImage)
        }

        val payload = MonthlyHighlightComposer.compose(month, inputs, members) ?: return false
        highlightRepository.saveAndFlush(
            MonthlyHighlight(
                groupRoomId = groupRoomId,
                targetMonth = targetMonth,
                totalDiaryCount = payload.totalDiaryCount,
                coverImageUrl = payload.coverImageUrl,
                payload = objectMapper.writeValueAsString(payload)
            )
        )
        log.info(
            "action=월간 하이라이트 생성, groupRoomId={}, month={}, diaryCount={}",
            groupRoomId,
            targetMonth,
            payload.totalDiaryCount
        )
        return true
    }
}
