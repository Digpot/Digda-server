package digdaserver.domain.deletionrequest.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 계정/데이터 삭제 요청. 디그팟 어드민(digda-admin)의 공개 페이지에서 비로그인 사용자가
 * 직접 작성한다(Google Play 데이터 안전성 정책의 계정·데이터 삭제 URL 요건 충족).
 *
 * 비로그인 접수라 사용자를 특정할 수 없으므로 [email] 로 본인 확인 후 어드민이 처리한다.
 * - ACCOUNT: 계정 전체 삭제 요청. [email] 만 받는다.
 * - DATA: 계정은 유지하고 일부 데이터 삭제 요청. [email] + [groupRoomName] + [content].
 */
@Entity
@Table(
    name = "deletion_request",
    indexes = [
        Index(name = "idx_deletion_request_status_created", columnList = "status, created_at")
    ]
)
class DeletionRequest(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deletion_request_id")
    val id: Long = 0L,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    val type: DeletionRequestType,

    @Column(name = "email", nullable = false, length = 255)
    val email: String,

    /** 데이터 삭제 요청 시 어떤 그룹방인지(계정 삭제면 null). */
    @Column(name = "group_room_name", length = 255)
    val groupRoomName: String? = null,

    /** 삭제를 원하는 데이터 설명(계정 삭제면 null). */
    @Column(name = "content", length = 2000)
    val content: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: DeletionRequestStatus = DeletionRequestStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "handled_at")
    var handledAt: LocalDateTime? = null
) {
    /** 어드민이 처리 완료로 전이. */
    fun markDone() {
        this.status = DeletionRequestStatus.DONE
        this.handledAt = LocalDateTime.now()
    }
}
