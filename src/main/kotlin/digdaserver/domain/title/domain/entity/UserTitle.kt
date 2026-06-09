package digdaserver.domain.title.domain.entity

import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

/**
 * 사용자가 획득한 칭호 1개. **계정(user) 소속**이라 그룹방 탈퇴/삭제와 무관하게 유지된다.
 *
 * 칭호의 표시 메타(이름·설명·아이콘·색)와 획득 조건 판정은 모두 **앱이 소유**한다.
 * 서버는 "누가 / 어떤 코드 / 어느 그룹방에서 / 언제" 땄는지를 저장·조회만 한다.
 *
 * - [code]          : 앱 카탈로그의 칭호 식별자(예: "region_gyeongnam", "diary_50", "char_evolved").
 * - [groupRoomId]   : 획득 당시 그룹방 id(지역/캐릭터 칭호). 전역 칭호(일기 수 등)는 null.
 * - [groupRoomName] : 획득 당시 그룹방 이름 **스냅샷**. 그룹이 삭제/탈퇴돼도 "OO 그룹방에서 따셨습니다" 가 남도록.
 */
@Entity
@Table(
    name = "user_title",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_title_user_code", columnNames = ["user_id", "code"])
    ],
    indexes = [
        Index(name = "idx_user_title_user", columnList = "user_id")
    ]
)
class UserTitle(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_title_id")
    val id: Long = 0L,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false, length = 40)
    val code: String,

    @Column(name = "group_room_id")
    val groupRoomId: Long? = null,

    @Column(name = "group_room_name", length = 100)
    val groupRoomName: String? = null,

    @Column(name = "earned_at", nullable = false)
    val earnedAt: LocalDateTime = LocalDateTime.now()

) : BaseTimeEntity()
