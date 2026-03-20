package digdaserver.domain.diary.domain.repository

import digdaserver.domain.diary.domain.entity.DiaryImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DiaryImageRepository : JpaRepository<DiaryImage, Long> {

    fun deleteAllByDiaryId(diaryId: Long)
}
