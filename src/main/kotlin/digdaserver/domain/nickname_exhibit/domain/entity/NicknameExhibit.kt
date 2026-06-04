package digdaserver.domain.nickname_exhibit.domain.entity

import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 디그다 역대 별명 전시관의 카드 1장. 재미용 콘텐츠라 그룹/유저에 묶이지 않는 전역 데이터.
 *
 * 앞면 = [imageUrl] + [nickname], 뒷면 = [history](별명이 생긴 배경·역사·설명).
 * 목록은 [sortOrder] 오름차순(동률 시 id 오름차순)으로 노출한다.
 * 등록/수정/삭제는 모두 어드민에서만 일어난다.
 */
@Entity
@Table(name = "nickname_exhibit")
class NicknameExhibit(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nickname_exhibit_id")
    val id: Long = 0L,

    @Column(nullable = false, length = 100)
    var nickname: String,

    @Column(name = "image_url", length = 512)
    var imageUrl: String? = null,

    @Column(columnDefinition = "TEXT")
    var history: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0

) : BaseTimeEntity() {

    /** 어드민 부분 수정 — null 인 필드는 변경하지 않는다. */
    fun update(nickname: String?, imageUrl: String?, history: String?, sortOrder: Int?) {
        nickname?.let { this.nickname = it }
        imageUrl?.let { this.imageUrl = it }
        history?.let { this.history = it }
        sortOrder?.let { this.sortOrder = it }
    }
}
