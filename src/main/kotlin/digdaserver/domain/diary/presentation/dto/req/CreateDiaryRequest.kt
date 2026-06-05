package digdaserver.domain.diary.presentation.dto.req

import java.time.LocalDate

data class CreateDiaryRequest(
    val title: String,
    val content: String,
    val date: LocalDate,
    val weather: Int,
    val mood: Int,
    val location: String? = null,
    /** 시그니처 지도 색칠용 정규 지역 키(앱 산출). 광역시=시도명, 도=시·군명. */
    val regionKey: String? = null,
    /** 표시용 시도명. */
    val regionSido: String? = null,
    /** 표시용 시군구명. */
    val regionSigungu: String? = null,
    /** UploadedImage.id (Long) 의 문자열 리스트. 0..10 장. 순서대로 sort_order 부여. */
    val imageIds: List<String> = emptyList()
)
