package digdaserver.domain.diary.domain.repository

import digdaserver.domain.diary.domain.entity.Diary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface DiaryRepository : JpaRepository<Diary, Long> {

    fun findAllByGroupRoomId(groupRoomId: Long, pageable: Pageable): Page<Diary>

    @Query("SELECT d FROM Diary d WHERE d.groupRoom.id = :groupRoomId AND d.date BETWEEN :startDate AND :endDate ORDER BY d.createdAt DESC")
    fun findAllByGroupRoomIdAndDateBetween(groupRoomId: Long, startDate: LocalDate, endDate: LocalDate, pageable: Pageable): Page<Diary>

    @Query("SELECT DISTINCT d.date FROM Diary d WHERE d.groupRoom.id = :groupRoomId AND d.date BETWEEN :startDate AND :endDate")
    fun findDistinctDatesByGroupRoomIdAndMonth(groupRoomId: Long, startDate: LocalDate, endDate: LocalDate): List<LocalDate>

    /**
     * 기간 내 모든 일기를 이미지까지 fetch join 으로 한 번에 로드한다.
     * 캘린더 그리드(날짜별 썸네일/기분)와 통계 계산용. 날짜 오름차순, 같은 날은 먼저 쓴 순
     * (대표 미지정 시 "가장 먼저 작성된 일기"가 기본 대표라 ASC 로 맞춘다).
     */
    @Query(
        "SELECT DISTINCT d FROM Diary d LEFT JOIN FETCH d.images " +
            "WHERE d.groupRoom.id = :groupRoomId AND d.date BETWEEN :startDate AND :endDate " +
            "ORDER BY d.date ASC, d.createdAt ASC"
    )
    fun findAllWithImagesByGroupRoomIdAndDateBetween(
        groupRoomId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Diary>

    /** 인당 하루 1편 검증 — 사용자가 그 그룹·그 날짜에 이미 일기를 썼는지. */
    fun existsByGroupRoomIdAndCreatedByIdAndDate(groupRoomId: Long, createdById: UUID, date: LocalDate): Boolean

    /** 인당 하루 1편 검증(수정용) — 본인 일기 자신은 제외하고 판정. */
    fun existsByGroupRoomIdAndCreatedByIdAndDateAndIdNot(
        groupRoomId: Long,
        createdById: UUID,
        date: LocalDate,
        id: Long
    ): Boolean

    /** 특정 날짜의 그룹 일기 전부(이미지 포함) — 날짜별 일기 목록 화면용. 먼저 쓴 순. */
    @Query(
        "SELECT DISTINCT d FROM Diary d LEFT JOIN FETCH d.images " +
            "WHERE d.groupRoom.id = :groupRoomId AND d.date = :date " +
            "ORDER BY d.createdAt ASC"
    )
    fun findAllWithImagesByGroupRoomIdAndDate(
        @Param("groupRoomId") groupRoomId: Long,
        @Param("date") date: LocalDate
    ): List<Diary>

    /** 대표 썸네일 재지정 — 같은 날의 기존 대표 플래그를 전부 내린다. */
    @Modifying
    @Query("UPDATE Diary d SET d.representative = false WHERE d.groupRoom.id = :groupRoomId AND d.date = :date AND d.representative = true")
    fun clearRepresentativeForDate(@Param("groupRoomId") groupRoomId: Long, @Param("date") date: LocalDate): Int

    @Query(
        """
        SELECT d FROM Diary d
        WHERE (:keyword IS NULL OR :keyword = ''
            OR LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(d.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """
    )
    fun searchForAdmin(
        @Param("keyword") keyword: String?,
        pageable: Pageable
    ): Page<Diary>

    @Modifying
    @Query("DELETE FROM Diary d WHERE d.createdBy.id = :userId")
    fun deleteAllByCreatedById(@Param("userId") userId: UUID)

    /** 칭호(작성 일기 수) — 사용자가 작성한 전체 일기 수(그룹 무관). */
    @Query("SELECT COUNT(d) FROM Diary d WHERE d.createdBy.id = :userId")
    fun countByCreatedById(@Param("userId") userId: UUID): Long

    /**
     * 시그니처 지도 — 그룹의 region_key 별 일기 수 집계. region_key 가 있는 일기만 대상.
     * 각 row = [regionKey(String), count(Long)].
     */
    @Query(
        "SELECT d.regionKey, COUNT(d) FROM Diary d " +
            "WHERE d.groupRoom.id = :groupRoomId AND d.regionKey IS NOT NULL " +
            "GROUP BY d.regionKey"
    )
    fun countByRegionKey(@Param("groupRoomId") groupRoomId: Long): List<Array<Any>>

    /**
     * 시그니처 지도(정복 칭호 판정용) — 특정 시점([since]) 이후 작성된 일기만 region_key 별 집계.
     * 중간 합류한 그룹원이 가입 전 색칠을 소급해서 칭호로 가져가지 못하게 가입 시각으로 거른다.
     */
    @Query(
        "SELECT d.regionKey, COUNT(d) FROM Diary d " +
            "WHERE d.groupRoom.id = :groupRoomId AND d.regionKey IS NOT NULL AND d.createdAt >= :since " +
            "GROUP BY d.regionKey"
    )
    fun countByRegionKeySince(
        @Param("groupRoomId") groupRoomId: Long,
        @Param("since") since: LocalDateTime
    ): List<Array<Any>>

    /** 칭호 백스톱 — [since] 이후 그룹에 region_key 가 있는 일기가 하나라도 있는지. */
    @Query(
        "SELECT COUNT(d) > 0 FROM Diary d " +
            "WHERE d.groupRoom.id = :groupRoomId AND d.regionKey IS NOT NULL AND d.createdAt >= :since"
    )
    fun existsRegionDiarySince(
        @Param("groupRoomId") groupRoomId: Long,
        @Param("since") since: LocalDateTime
    ): Boolean

    /** 시그니처 지도 — 특정 region_key 의 그룹 일기 목록(최신순). */
    @Query("SELECT d FROM Diary d WHERE d.groupRoom.id = :groupRoomId AND d.regionKey = :regionKey ORDER BY d.date DESC, d.createdAt DESC")
    fun findAllByGroupRoomIdAndRegionKey(
        @Param("groupRoomId") groupRoomId: Long,
        @Param("regionKey") regionKey: String,
        pageable: Pageable
    ): Page<Diary>
}
