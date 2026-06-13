package digdaserver.domain.block.application.service.impl

import digdaserver.domain.block.application.service.BlockService
import digdaserver.domain.block.domain.entity.ContentHide
import digdaserver.domain.block.domain.entity.HideReason
import digdaserver.domain.block.domain.entity.HideTargetType
import digdaserver.domain.block.domain.entity.UserBlock
import digdaserver.domain.block.domain.repository.ContentHideRepository
import digdaserver.domain.block.domain.repository.UserBlockRepository
import digdaserver.domain.block.presentation.dto.res.BlockedUserResponse
import digdaserver.domain.log.application.service.UserActionLogService
import digdaserver.domain.log.domain.entity.UserAction
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BlockServiceImpl(
    private val userBlockRepository: UserBlockRepository,
    private val contentHideRepository: ContentHideRepository,
    private val userRepository: UserRepository,
    private val userActionLogService: UserActionLogService
) : BlockService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun blockUser(blockerId: UUID, blockedUserId: UUID) {
        if (blockerId == blockedUserId) throw DigdaException(ErrorCode.CANNOT_BLOCK_SELF)

        val blocker = userRepository.findById(blockerId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }
        val blocked = userRepository.findById(blockedUserId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedUserId)) {
            log.info("action=사용자 차단 멱등 스킵(이미 차단됨), blockerId={}, blockedId={}", blockerId, blockedUserId)
            return
        }
        userBlockRepository.save(UserBlock(blocker = blocker, blocked = blocked))
        userActionLogService.record(
            actorId = blockerId,
            action = UserAction.BLOCK_USER,
            targetType = "USER",
            targetId = blockedUserId.toString(),
            detail = null
        )
        log.info("action=사용자 차단, blockerId={}, blockedId={}", blockerId, blockedUserId)
    }

    @Transactional
    override fun unblockUser(blockerId: UUID, blockedUserId: UUID) {
        val block = userBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedUserId)
        if (block == null) {
            log.info("action=차단 해제 멱등 스킵(차단 아님), blockerId={}, blockedId={}", blockerId, blockedUserId)
            return
        }
        userBlockRepository.delete(block)
        userActionLogService.record(
            actorId = blockerId,
            action = UserAction.UNBLOCK_USER,
            targetType = "USER",
            targetId = blockedUserId.toString(),
            detail = null
        )
        log.info("action=차단 해제, blockerId={}, blockedId={}", blockerId, blockedUserId)
    }

    override fun listBlockedUsers(blockerId: UUID): List<BlockedUserResponse> {
        return userBlockRepository.findAllByBlockerIdWithBlocked(blockerId)
            .map { BlockedUserResponse.from(it) }
    }

    @Transactional
    override fun hideContent(userId: UUID, targetType: HideTargetType, targetId: Long, reason: HideReason) {
        if (contentHideRepository.existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)) {
            log.info(
                "action=콘텐츠 숨김 멱등 스킵(이미 숨김), userId={}, type={}, targetId={}",
                userId,
                targetType,
                targetId
            )
            return
        }
        contentHideRepository.save(
            ContentHide(userId = userId, targetType = targetType, targetId = targetId, reason = reason)
        )
        userActionLogService.record(
            actorId = userId,
            action = UserAction.HIDE_CONTENT,
            targetType = targetType.name,
            targetId = targetId.toString(),
            detail = "reason=$reason"
        )
        log.info("action=콘텐츠 숨김, userId={}, type={}, targetId={}, reason={}", userId, targetType, targetId, reason)
    }

    @Transactional
    override fun unhideContent(userId: UUID, targetType: HideTargetType, targetId: Long) {
        val hide = contentHideRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
        if (hide == null) {
            log.info(
                "action=콘텐츠 숨김 해제 멱등 스킵(숨김 아님), userId={}, type={}, targetId={}",
                userId,
                targetType,
                targetId
            )
            return
        }
        contentHideRepository.delete(hide)
        log.info("action=콘텐츠 숨김 해제, userId={}, type={}, targetId={}", userId, targetType, targetId)
    }
}
