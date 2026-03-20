package digdaserver.domain.user.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "user_terms")
class UserTerms(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_terms_id")
    val id: Long = 0L,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(name = "terms_of_service", nullable = false)
    val termsOfService: Boolean,

    @Column(name = "privacy_policy", nullable = false)
    val privacyPolicy: Boolean,

    @Column(name = "age_confirmation", nullable = false)
    val ageConfirmation: Boolean,

    @Column(name = "marketing_consent", nullable = false)
    var marketingConsent: Boolean = false,

    @Column(name = "push_consent", nullable = false)
    var pushConsent: Boolean = false,

    @Column(name = "agreed_at", nullable = false)
    val agreedAt: LocalDateTime = LocalDateTime.now()
)
