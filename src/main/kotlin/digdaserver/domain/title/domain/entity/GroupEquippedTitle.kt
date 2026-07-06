package digdaserver.domain.title.domain.entity

import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

/**
 * 그룹 모찌에 장착된 칭호(그룹당 1개). 모찌는 그룹 공용 캐릭터라 장착도 그룹 단위다.
 *
 * 멤버 누구든 **자신이 획득한** 칭호([UserTitle]) 중 하나를 그룹 모찌에 장착할 수 있고,
 * 마지막에 장착한 값이 그룹 모찌 화면에 표시된다. 해제는 row 삭제.
 */
@Entity
@Table(
    name = "group_equipped_title",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_group_equipped_title_group", columnNames = ["group_room_id"])
    ]
)
class GroupEquippedTitle(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_equipped_title_id")
    val id: Long = 0L,

    @Column(name = "group_room_id", nullable = false)
    val groupRoomId: Long,

    @Column(nullable = false, length = 40)
    var code: String,

    /** 장착한 사용자(소유자 검증·표시용). */
    @Column(name = "equipped_by", nullable = false)
    var equippedBy: UUID

) : BaseTimeEntity() {

    fun replaceWith(code: String, userId: UUID) {
        this.code = code
        this.equippedBy = userId
    }
}
