package digdaserver.domain.character.application.service.impl

import digdaserver.domain.character.application.service.CharacterQuizService
import digdaserver.domain.character.domain.entity.CharacterQuiz
import digdaserver.domain.character.domain.entity.CharacterQuizAttempt
import digdaserver.domain.character.domain.entity.GroupCharacter
import digdaserver.domain.character.domain.repository.CharacterQuizAttemptRepository
import digdaserver.domain.character.domain.repository.CharacterQuizRepository
import digdaserver.domain.character.domain.repository.GroupCharacterEquippedRepository
import digdaserver.domain.character.domain.repository.GroupCharacterRepository
import digdaserver.domain.character.presentation.dto.req.CreateQuizRequest
import digdaserver.domain.character.presentation.dto.res.CharacterQuizListResponse
import digdaserver.domain.character.presentation.dto.res.CharacterQuizResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStateResponse
import digdaserver.domain.character.presentation.dto.res.QuizAttemptResultResponse
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CharacterQuizServiceImpl(
    private val quizRepository: CharacterQuizRepository,
    private val attemptRepository: CharacterQuizAttemptRepository,
    private val groupCharacterRepository: GroupCharacterRepository,
    private val groupCharacterEquippedRepository: GroupCharacterEquippedRepository,
    private val gearInitializer: CharacterGearInitializer,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository,
    @Lazy private val notificationService: NotificationService
) : CharacterQuizService {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val QUESTION_MAX = 200
        private const val OPTION_MAX = 100
        private const val OPTION_COUNT = 4
        private const val MULTIPLIER_MIN = 1
        private const val MULTIPLIER_MAX = 3
        private const val EXP_PER_MULTIPLIER_CORRECT = 30
        private const val COIN_PER_MULTIPLIER_CORRECT = 5
        private const val EXP_CONSOLATION_WRONG = 5
    }

    @Transactional
    override fun createQuiz(
        userId: UUID,
        request: CreateQuizRequest
    ): CharacterQuizResponse {
        validateQuizPayload(request)
        validateGroupMember(request.groupRoomId, userId)

        val groupRoom = groupRoomRepository.findById(request.groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        val author = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val normalizedImageUrl = request.imageUrl?.trim()?.ifEmpty { null }
        // 사진 퀴즈는 디코가 등장한 그룹에서만 등록할 수 있다. 디코는 모찌의 사진 퀴즈
        // 메이트 컨셉이라, 디코 없는 그룹은 텍스트 퀴즈만 운영하도록 게이트.
        if (normalizedImageUrl != null) {
            val character = groupCharacterRepository.findByGroupRoomId(request.groupRoomId)
            if (character == null || !character.dikoUnlocked) {
                throw DigdaException(ErrorCode.QUIZ_IMAGE_REQUIRES_DIKO)
            }
        }
        val quiz = quizRepository.save(
            CharacterQuiz(
                groupRoom = groupRoom,
                author = author,
                category = request.category,
                question = request.question,
                option1 = request.options[0],
                option2 = request.options[1],
                option3 = request.options[2],
                option4 = request.options[3],
                correctIndex = request.correctIndex,
                expMultiplier = request.expMultiplier,
                imageUrl = normalizedImageUrl
            )
        )
        log.info(
            "action=character_quiz_create, userId={}, quizId={}, groupRoomId={}, category={}, multiplier={}",
            userId,
            quiz.id,
            request.groupRoomId,
            request.category,
            request.expMultiplier
        )

        try {
            notificationService.notifyQuizCreated(
                groupRoomId = request.groupRoomId,
                quizId = quiz.id,
                authorUserId = userId,
                question = quiz.question
            )
        } catch (e: Exception) {
            log.warn("action=character_quiz_create_notify_failed, quizId={}, error={}", quiz.id, e.message)
        }

        return CharacterQuizResponse.from(quiz)
    }

    @Transactional
    override fun listQuizzes(
        userId: UUID,
        groupRoomId: Long,
        page: Int,
        size: Int
    ): CharacterQuizListResponse {
        validateGroupMember(groupRoomId, userId)
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)
        val result =
            quizRepository.findPageByGroupRoomId(groupRoomId, PageRequest.of(safePage, safeSize))
        return CharacterQuizListResponse(
            items = result.content.map(CharacterQuizResponse::from),
            page = result.number,
            totalPages = result.totalPages,
            totalElements = result.totalElements
        )
    }

    @Transactional
    override fun pickRandom(userId: UUID, groupRoomId: Long): CharacterQuizResponse {
        validateGroupMember(groupRoomId, userId)
        // 디코가 없는 그룹은 사진 퀴즈를 후보에서 제외 — 사용자에게 풀 수 없는 문제를
        // 띄우면 안 되므로 repository 레벨에서 차단.
        val excludeImage = !isDikoUnlocked(groupRoomId)
        val candidates = quizRepository.findAvailableForUser(groupRoomId, userId, excludeImage)
        if (candidates.isEmpty()) throw DigdaException(ErrorCode.QUIZ_NO_AVAILABLE)
        // DB 종속 RAND() 회피 — JPQL 로 후보 가져온 뒤 Kotlin random.
        val picked = candidates.random()
        log.info(
            "action=character_quiz_pick, userId={}, quizId={}, groupRoomId={}, excludeImage={}",
            userId,
            picked.id,
            groupRoomId,
            excludeImage
        )
        return CharacterQuizResponse.from(picked)
    }

    private fun isDikoUnlocked(groupRoomId: Long): Boolean {
        return groupCharacterRepository.findByGroupRoomId(groupRoomId)?.dikoUnlocked == true
    }

    @Transactional
    override fun submitAttempt(
        userId: UUID,
        quizId: Long,
        selectedIndex: Int
    ): QuizAttemptResultResponse {
        if (selectedIndex !in 1..OPTION_COUNT) {
            throw DigdaException(ErrorCode.QUIZ_INVALID_CORRECT_INDEX)
        }

        val quiz = quizRepository.findById(quizId)
            .orElseThrow { DigdaException(ErrorCode.QUIZ_NOT_FOUND) }

        if (quiz.author.id == userId) throw DigdaException(ErrorCode.QUIZ_CANNOT_ATTEMPT_OWN)
        validateGroupMember(quiz.groupRoom.id, userId)

        // 사진 퀴즈는 디코가 풀린 그룹에서만 응시 가능. URL 우회 방어용.
        if (quiz.imageUrl != null && !isDikoUnlocked(quiz.groupRoom.id)) {
            throw DigdaException(ErrorCode.QUIZ_IMAGE_REQUIRES_DIKO)
        }

        if (attemptRepository.existsByQuizIdAndUserId(quizId, userId)) {
            throw DigdaException(ErrorCode.QUIZ_ALREADY_ATTEMPTED)
        }

        val correct = selectedIndex == quiz.correctIndex
        val earnedExp = if (correct) EXP_PER_MULTIPLIER_CORRECT * quiz.expMultiplier else EXP_CONSOLATION_WRONG
        val earnedCoin = if (correct) COIN_PER_MULTIPLIER_CORRECT * quiz.expMultiplier else 0

        // 보상은 그룹 캐릭터에 누적 (응시자 개인이 아닌 그룹 공용)
        val character = loadOrCreateGroupCharacter(quiz.groupRoom.id)
        val gain = character.gainExp(earnedExp)
        if (earnedCoin > 0) character.addCoin(earnedCoin)

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }
        attemptRepository.save(
            CharacterQuizAttempt(
                quiz = quiz,
                user = user,
                selectedIndex = selectedIndex,
                correct = correct,
                earnedExp = earnedExp,
                earnedCoin = earnedCoin
            )
        )

        log.info(
            "action=character_quiz_attempt, userId={}, groupRoomId={}, quizId={}, selected={}, " +
                "correct={}, earnedExp={}, earnedCoin={}, levelGained={}, stageChanged={}",
            userId, quiz.groupRoom.id, quizId, selectedIndex,
            correct, earnedExp, earnedCoin, gain.levelGained, gain.stageChanged
        )

        try {
            if (correct) {
                notificationService.notifyQuizAnsweredCorrectly(
                    groupRoomId = quiz.groupRoom.id,
                    quizId = quizId,
                    solverUserId = userId
                )
            }
            if (gain.levelGained > 0 || gain.stageChanged) {
                notificationService.notifyMochiLevelUp(
                    groupRoomId = quiz.groupRoom.id,
                    actorUserId = userId,
                    newLevel = character.level,
                    stageChanged = gain.stageChanged,
                    stageName = gain.stageAfter.displayName
                )
            }
            if (gain.dikoJustUnlocked) {
                notificationService.notifyDikoUnlocked(
                    groupRoomId = quiz.groupRoom.id,
                    actorUserId = userId
                )
            }
        } catch (e: Exception) {
            log.warn(
                "action=character_quiz_attempt_notify_failed, quizId={}, error={}",
                quizId,
                e.message
            )
        }

        val equipped = groupCharacterEquippedRepository.findAllByGroupRoomId(quiz.groupRoom.id)
        return QuizAttemptResultResponse(
            quizId = quiz.id,
            correct = correct,
            correctIndex = quiz.correctIndex,
            selectedIndex = selectedIndex,
            earnedExp = earnedExp,
            earnedCoin = earnedCoin,
            character = CharacterStateResponse.from(character, equipped),
            levelGained = gain.levelGained,
            stageBefore = gain.stageBefore,
            stageAfter = gain.stageAfter,
            stageChanged = gain.stageChanged,
            dikoJustUnlocked = gain.dikoJustUnlocked
        )
    }

    private fun validateQuizPayload(request: CreateQuizRequest) {
        if (request.options.size != OPTION_COUNT) {
            throw DigdaException(ErrorCode.QUIZ_INVALID_OPTION_COUNT)
        }
        if (request.question.isBlank() || request.question.length > QUESTION_MAX) {
            throw DigdaException(ErrorCode.QUIZ_QUESTION_INVALID)
        }
        request.options.forEachIndexed { idx, opt ->
            if (opt.isBlank() || opt.length > OPTION_MAX) {
                log.warn("action=character_quiz_create_invalid_option, index={}", idx)
                throw DigdaException(ErrorCode.QUIZ_OPTION_INVALID)
            }
        }
        if (request.correctIndex !in 1..OPTION_COUNT) {
            throw DigdaException(ErrorCode.QUIZ_INVALID_CORRECT_INDEX)
        }
        if (request.expMultiplier !in MULTIPLIER_MIN..MULTIPLIER_MAX) {
            throw DigdaException(ErrorCode.QUIZ_INVALID_MULTIPLIER)
        }
    }

    private fun validateGroupMember(groupRoomId: Long, userId: UUID) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)
        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }
    }

    private fun loadOrCreateGroupCharacter(groupRoomId: Long): GroupCharacter {
        val existing = groupCharacterRepository.findByGroupRoomId(groupRoomId)
        if (existing != null) {
            gearInitializer.ensureDefaults(existing)
            return existing
        }
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        val fresh = groupCharacterRepository.save(GroupCharacter(groupRoom = groupRoom))
        gearInitializer.ensureDefaults(fresh)
        log.info(
            "action=character_create_via_quiz, groupRoomId={}, characterId={}",
            groupRoomId,
            fresh.id
        )
        return fresh
    }
}
