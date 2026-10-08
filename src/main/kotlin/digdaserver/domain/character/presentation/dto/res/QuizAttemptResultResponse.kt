package digdaserver.domain.character.presentation.dto.res

import digdaserver.domain.character.domain.entity.CharacterStage

/**
 * 퀴즈 응시 결과 + 보상 적용 후 갱신된 캐릭터 상태.
 *
 * 클라이언트는 [correct], [correctIndex] 로 결과 화면을 만들고,
 * [character], [levelGained], [stageChanged] 로 레벨업/진화 연출을 띄운다.
 *
 * 시즌 이벤트(경험치 N배) 진행 중이면 [earnedExp] 는 이미 배수가 적용된 값이고,
 * [expMultiplier]/[bonusExp] 로 "이벤트 덕에 +N" 을 따로 보여줄 수 있다.
 */
data class QuizAttemptResultResponse(
    val quizId: Long,
    val correct: Boolean,
    val correctIndex: Int,
    val selectedIndex: Int,
    val earnedExp: Int,
    val earnedCoin: Int,
    val character: CharacterStateResponse,
    val levelGained: Int,
    val stageBefore: CharacterStage,
    val stageAfter: CharacterStage,
    val stageChanged: Boolean,
    val dikoJustUnlocked: Boolean = false,
    val expMultiplier: Double = 1.0,
    val bonusExp: Int = 0
)
