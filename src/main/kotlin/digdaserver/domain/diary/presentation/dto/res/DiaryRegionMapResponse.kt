package digdaserver.domain.diary.presentation.dto.res

/**
 * 시그니처 지도 — 그룹의 지역별 일기 수 집계 응답.
 *
 * 색칠 임계값(군·시=1, 광역시=10) 판정은 앱이 로컬 지도 에셋의 metro 플래그로 수행하므로,
 * 서버는 region_key 별 raw count 만 내려준다.
 */
data class DiaryRegionMapResponse(
    val regions: List<DiaryRegionCount>,
    /** region_key 가 지정된 전체 일기 수(분류된 기록 총합). */
    val total: Long
)

data class DiaryRegionCount(
    val regionKey: String,
    val count: Long
)
