package digdaserver.domain.notification.domain.repository

import digdaserver.domain.notification.domain.entity.Notification
import digdaserver.domain.notification.domain.entity.NotificationType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<Notification>

    fun countByUserIdAndIsReadFalse(userId: UUID): Int

    /**
     * 특정 일정에 대해 해당 종류의 리마인더가 [after] 이후에 발송됐는지 확인.
     * 멀티데이 당일 리마인더를 '날마다 1회'로 보내기 위해, 전역 1회가 아니라 시간창(예: 12h)
     * 안에서만 중복으로 본다 — 같은 날 여러 슬롯(09/12/18시)은 막고, 다음 날엔 다시 보낸다.
     */
    fun existsByTypeAndRelatedIdAndCreatedAtAfter(
        type: NotificationType,
        relatedId: Long,
        after: LocalDateTime
    ): Boolean

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    fun markAllAsReadByUserId(userId: UUID): Int

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    fun deleteAllByUserId(@Param("userId") userId: UUID)

    /**
     * 어드민 전체 알림 검색.
     *
     * - [types]: null/빈 리스트 = 전체. 모찌 관련 알림만 보고 싶을 때 호출 측에서
     *   `[MOCHI_LEVELUP, DIKO_UNLOCKED, QUIZ_CREATED, QUIZ_ANSWERED]` 같이 전달.
     * - [groupRoomId]: null = 전체 그룹.
     * - [keyword]: 제목/본문 LIKE.
     *
     * 사용자별로 같은 이벤트가 여러 행으로 저장되는 점은 admin 화면에서 그대로 노출한다
     * (어드민은 raw 데이터를 봐야 의미가 있음).
     */
    @Query(
        """
        SELECT n FROM Notification n
        WHERE (:types IS NULL OR n.type IN :types)
          AND (:groupRoomId IS NULL OR n.groupRoomId = :groupRoomId)
          AND (:keyword IS NULL OR :keyword = ''
              OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(n.message) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """
    )
    fun searchForAdmin(
        @Param("types") types: Collection<NotificationType>?,
        @Param("groupRoomId") groupRoomId: Long?,
        @Param("keyword") keyword: String?,
        pageable: Pageable
    ): Page<Notification>
}
