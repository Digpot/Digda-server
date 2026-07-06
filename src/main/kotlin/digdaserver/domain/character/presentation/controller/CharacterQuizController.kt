package digdaserver.domain.character.presentation.controller

import digdaserver.domain.character.application.service.CharacterQuizService
import digdaserver.domain.character.presentation.dto.req.CreateQuizRequest
import digdaserver.domain.character.presentation.dto.req.SubmitAttemptRequest
import digdaserver.domain.character.presentation.dto.res.CharacterQuizListResponse
import digdaserver.domain.character.presentation.dto.res.CharacterQuizResponse
import digdaserver.domain.character.presentation.dto.res.QuizAttemptResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/character-quizzes")
@Tag(name = "CharacterQuiz", description = "캐릭터(모찌) 퀴즈 API")
class CharacterQuizController(
    private val characterQuizService: CharacterQuizService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "퀴즈 생성", description = "그룹 멤버만 가능. 4지선다 + EXP 배수(1-3).")
    @PostMapping
    fun create(
        @AuthenticationPrincipal userId: String,
        @RequestBody request: CreateQuizRequest
    ): ResponseEntity<CharacterQuizResponse> {
        log.info(
            "api=POST /character-quizzes, userId={}, groupRoomId={}, category={}",
            userId,
            request.groupRoomId,
            request.category
        )
        val response = characterQuizService.createQuiz(UUID.fromString(userId), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "그룹 퀴즈 목록", description = "그룹 내 퀴즈를 최신순 페이지네이션으로 반환.")
    @GetMapping
    fun list(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<CharacterQuizListResponse> {
        log.info("api=GET /character-quizzes, userId={}, groupRoomId={}, page={}", userId, groupRoomId, page)
        return ResponseEntity.ok(
            characterQuizService.listQuizzes(UUID.fromString(userId), groupRoomId, page, size)
        )
    }

    @Operation(summary = "랜덤 풀기 후보", description = "내가 작성하지 않았고 아직 풀지 않은 퀴즈 1건을 랜덤으로 반환.")
    @GetMapping("/random")
    fun random(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long
    ): ResponseEntity<CharacterQuizResponse> {
        log.info("api=GET /character-quizzes/random, userId={}, groupRoomId={}", userId, groupRoomId)
        return ResponseEntity.ok(
            characterQuizService.pickRandom(UUID.fromString(userId), groupRoomId)
        )
    }

    @Operation(summary = "퀴즈 응시", description = "정답 여부 + 보상 + 갱신된 캐릭터 상태를 한 번에 반환.")
    @PostMapping("/{quizId}/attempt")
    fun attempt(
        @AuthenticationPrincipal userId: String,
        @PathVariable quizId: Long,
        @RequestBody request: SubmitAttemptRequest
    ): ResponseEntity<QuizAttemptResultResponse> {
        log.info(
            "api=POST /character-quizzes/{}/attempt, userId={}, selected={}",
            quizId,
            userId,
            request.selectedIndex
        )
        return ResponseEntity.ok(
            characterQuizService.submitAttempt(
                UUID.fromString(userId),
                quizId,
                request.selectedIndex,
                request.practice
            )
        )
    }
}
