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
 * - 강제 업데이트: [minAppVersion] 보다 낮은 버전의 앱은 스토어 이동 다이얼로그로 막는다.
 *   빈 문자열이면 게이트 비활성. 스토어 URL 이 비어 있으면 앱이 기본 URL 로 폴백.
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
    var feedbackUrl: String = "",

    /** 강제 업데이트 최소 버전(semver "2.0.0"). 빈 값 = 게이트 끔. */
    @Column(name = "min_app_version", nullable = false, length = 20)
    var minAppVersion: String = "",

    /** 안드로이드 스토어 URL. 빈 값이면 앱이 Play 기본 URL 로 폴백. */
    @Column(name = "store_url_android", nullable = false, length = 500)
    var storeUrlAndroid: String = "",

    /** iOS App Store URL. 빈 값이면 앱이 안내 문구만 노출. */
    @Column(name = "store_url_ios", nullable = false, length = 500)
    var storeUrlIos: String = ""

) : BaseTimeEntity() {

    /**
     * 어드민 저장. 버전/스토어 필드는 구버전 어드민 UI 가 보내지 않을 수 있어
     * null 이면 기존 값을 유지한다(의도치 않은 초기화 방지).
     */
    fun update(
        noticeEnabled: Boolean,
        noticeMessage: String,
        feedbackEnabled: Boolean,
        feedbackUrl: String,
        minAppVersion: String? = null,
        storeUrlAndroid: String? = null,
        storeUrlIos: String? = null
    ) {
        this.noticeEnabled = noticeEnabled
        this.noticeMessage = noticeMessage
        this.feedbackEnabled = feedbackEnabled
        this.feedbackUrl = feedbackUrl
        minAppVersion?.let { this.minAppVersion = it }
        storeUrlAndroid?.let { this.storeUrlAndroid = it }
        storeUrlIos?.let { this.storeUrlIos = it }
    }
}
