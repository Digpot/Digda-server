package digdaserver.domain.character.application.service

import digdaserver.domain.character.presentation.dto.req.CreateQuizRequest
import digdaserver.domain.character.presentation.dto.res.CharacterQuizListResponse
import digdaserver.domain.character.presentation.dto.res.CharacterQuizResponse
import digdaserver.domain.character.presentation.dto.res.QuizAttemptResultResponse
import java.util.UUID

interface CharacterQuizService {

    /** 퀴즈 생성. 작성자는 그룹 멤버여야 함. */
    fun createQuiz(userId: UUID, request: CreateQuizRequest): CharacterQuizResponse

    /** 그룹의 퀴즈 목록 (최신순 페이지네이션). */
    fun listQuizzes(
        userId: UUID,
        groupRoomId: Long,
        page: Int,
        size: Int
    ): CharacterQuizListResponse

    /** 풀기 가능한(자기 작성 X, 미응시) 퀴즈 1건 랜덤. 없으면 4xx. */
    fun pickRandom(userId: UUID, groupRoomId: Long): CharacterQuizResponse

    /**
     * 퀴즈 응시. 보상 자동 적용 후 캐릭터 상태 포함 응답.
     * [practice] 가 true 면 재풀이(연습) 모드 — 이미 푼 문제·본인 출제도 가능하고
     * 보상(경험치/코인)·기록·알림 없이 정답 여부만 돌려준다.
     */
    fun submitAttempt(
        userId: UUID,
        quizId: Long,
        selectedIndex: Int,
        practice: Boolean = false
    ): QuizAttemptResultResponse
}
