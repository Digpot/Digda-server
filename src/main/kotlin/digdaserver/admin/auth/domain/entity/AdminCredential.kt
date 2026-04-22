package digdaserver.admin.auth.domain.entity

import digdaserver.domain.user.domain.entity.User
import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "admin_credential",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_admin_credential_email", columnNames = ["email"]),
        UniqueConstraint(name = "uk_admin_credential_user", columnNames = ["user_id"])
    ]
)
class AdminCredential(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_credential_id")
    val id: Long = 0L,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, length = 100)
    var email: String,

    @Column(nullable = false, length = 100)
    var password: String

) : BaseTimeEntity() {

    fun updatePassword(encodedPassword: String) {
        this.password = encodedPassword
    }
}
