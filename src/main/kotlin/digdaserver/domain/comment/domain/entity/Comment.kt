package digdaserver.domain.comment.domain.entity

import digdaserver.domain.user.domain.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "comment",
    indexes = [Index(name = "idx_comment_target", columnList = "target_type, target_id")]
)
class Comment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    val id: Long = 0L,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    val targetType: CommentTargetType,

    @Column(name = "target_id", nullable = false)
    val targetId: Long,

    @Column(nullable = false, length = 200)
    val text: String,

    /**
     * 대댓글의 부모 댓글 id. null 이면 최상위 댓글.
     * 구조는 댓글 → 대댓글 1단계까지만 허용한다(대대댓글 금지 — 서비스에서 검증).
     * prod 는 SchemaAutoMigration 에서 동일 컬럼을 멱등 ADD.
     */
    @Column(name = "parent_comment_id")
    val parentId: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: User,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
