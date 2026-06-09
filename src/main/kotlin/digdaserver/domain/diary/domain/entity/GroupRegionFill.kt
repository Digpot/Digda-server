package digdaserver.domain.diary.domain.entity

import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 어드민이 임의로 "채운" 지역 1칸(그룹×지역키). 실제 일기와 무관하게 시그니처 지도를 칠하기 위한 override.
 *
 * 시그니처 지도 응답(region-map)은 일기 집계에 이 override 를 병합해, 해당 region_key 를
 * 색칠 임계 이상으로 보이게 만든다. 행 삭제 = 채움 해제.
 */
@Entity
@Table(
    name = "group_region_fill",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_group_region_fill", columnNames = ["group_room_id", "region_key"])
    ]
)
class GroupRegionFill(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_region_fill_id")
    val id: Long = 0L,

    @Column(name = "group_room_id", nullable = false)
    val groupRoomId: Long,

    @Column(name = "region_key", nullable = false, length = 40)
    val regionKey: String

) : BaseTimeEntity()
