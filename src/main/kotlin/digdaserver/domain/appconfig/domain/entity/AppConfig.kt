package digdaserver.domain.appconfig.domain.entity

import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 앱 전역 운영 설정 — 어드민이 토글/입력하는 **단일 행**.
 *
 * - 대공지(전광판): [noticeEnabled] && [noticeMessage] 가 있으면 그룹홈 상단에 흐른다.
 * - 피드백: [feedbackEnabled] 면 마이페이지에 "피드백 받기" 노출, [feedbackUrl] 로 이동.
 */
@Entity
@Table(name = "app_config")
class AppConfig(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_config_id")
    val id: Long = 0L,

    @Column(name = "notice_enabled", nullable = false)
    var noticeEnabled: Boolean = false,

    @Column(name = "notice_message", nullable = false, length = 200)
    var noticeMessage: String = "",

    @Column(name = "feedback_enabled", nullable = false)
    var feedbackEnabled: Boolean = false,

    @Column(name = "feedback_url", nullable = false, length = 500)
    var feedbackUrl: String = ""

) : BaseTimeEntity() {

    fun update(
        noticeEnabled: Boolean,
        noticeMessage: String,
        feedbackEnabled: Boolean,
        feedbackUrl: String
    ) {
        this.noticeEnabled = noticeEnabled
        this.noticeMessage = noticeMessage
        this.feedbackEnabled = feedbackEnabled
        this.feedbackUrl = feedbackUrl
    }
}
