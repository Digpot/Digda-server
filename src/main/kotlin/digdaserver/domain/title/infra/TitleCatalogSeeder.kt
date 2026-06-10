package digdaserver.domain.title.infra

import digdaserver.domain.title.domain.entity.TitleCatalogEntry
import digdaserver.domain.title.domain.repository.TitleCatalogRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 칭호 카탈로그 마스터 데이터 idempotent 시드(모찌 ShopItemSeeder 와 동일 운영).
 * 데이터를 날려도 `ddl-auto=create` + 이 시드로 카탈로그가 재생성된다. [code] 가 키.
 *
 * **앱의 하드코딩 TitleCatalog(코드/색/아이콘)와 코드가 일치해야 한다.**
 */
@Component
class TitleCatalogSeeder(
    private val titleCatalogRepository: TitleCatalogRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun seed() {
        var created = 0
        var updated = 0
        SeedCatalog.all.forEach { d ->
            val existing = titleCatalogRepository.findByCode(d.code)
            if (existing == null) {
                titleCatalogRepository.save(
                    TitleCatalogEntry(
                        code = d.code,
                        name = d.name,
                        description = d.description,
                        category = d.category,
                        accentColor = d.accentColor,
                        iconKey = d.iconKey,
                        conditionType = d.conditionType,
                        conditionValue = d.conditionValue,
                        sortOrder = d.sortOrder
                    )
                )
                created += 1
            } else {
                existing.updateMeta(
                    name = d.name,
                    description = d.description,
                    category = d.category,
                    accentColor = d.accentColor,
                    iconKey = d.iconKey,
                    conditionType = d.conditionType,
                    conditionValue = d.conditionValue,
                    sortOrder = d.sortOrder
                )
                updated += 1
            }
        }
        log.info("action=title_catalog_seed_done, created={}, updated={}", created, updated)
    }

    data class Def(
        val code: String,
        val name: String,
        val description: String,
        val category: String,
        val accentColor: String,
        val iconKey: String,
        val conditionType: String,
        val conditionValue: String?,
        val sortOrder: Int
    )

    object SeedCatalog {
        val all: List<Def> = listOf(
            // ─── 지역 정복(region) — conditionValue = 지도 색칠 키/버킷명 ───
            // 광역시·특별시·특별자치시 — 도시별 '시장' 칭호(해당 도시 임계 채움).
            Def("region_seoul", "서울특별시장", "서울을 일기로 가득 채웠어요", "region", "#FF8A5B", "location_city", "region", "서울", 10),
            Def("region_busan", "부산광역시장", "부산을 일기로 가득 채웠어요", "region", "#4FA8E0", "location_city", "region", "부산", 11),
            Def("region_daegu", "대구광역시장", "대구를 일기로 가득 채웠어요", "region", "#F47B6A", "location_city", "region", "대구", 12),
            Def("region_incheon", "인천광역시장", "인천을 일기로 가득 채웠어요", "region", "#5BC0BE", "location_city", "region", "인천", 13),
            Def("region_gwangju", "광주광역시장", "광주를 일기로 가득 채웠어요", "region", "#9B7EDE", "location_city", "region", "광주", 14),
            Def("region_daejeon", "대전광역시장", "대전을 일기로 가득 채웠어요", "region", "#4DB6AC", "location_city", "region", "대전", 15),
            Def("region_ulsan", "울산광역시장", "울산을 일기로 가득 채웠어요", "region", "#E08A4F", "location_city", "region", "울산", 16),
            Def("region_sejong", "세종특별자치시장", "세종을 일기로 가득 채웠어요", "region", "#7C9CD0", "location_city", "region", "세종", 17),
            // 전국 대도시 석권 — 그랜드 칭호(모든 광역시·특별시 채움).
            Def("region_metro", "대도시 정복자", "전국 대도시를 모두 채웠어요", "region", "#FF6B6B", "location_city", "region", "광역시", 18),
            // 도(道) — '도지사' 칭호(권역 전체 시·군 채움).
            Def("region_gyeonggi_north", "경기북부 도지사", "경기 북부의 모든 시·군을 채웠어요", "region", "#FF6B6B", "flag", "region", "경기북부", 20),
            Def("region_gyeonggi_south", "경기남부 도지사", "경기 남부의 모든 시·군을 채웠어요", "region", "#E8553D", "flag", "region", "경기남부", 30),
            Def("region_gangwon", "강원도지사", "강원의 모든 시·군을 채웠어요", "region", "#5B9BF0", "flag", "region", "강원", 40),
            Def("region_chungbuk", "충청북도지사", "충청북도의 모든 시·군을 채웠어요", "region", "#F4B53C", "flag", "region", "충북", 50),
            Def("region_chungnam", "충청남도지사", "충청남도의 모든 시·군을 채웠어요", "region", "#E0962B", "flag", "region", "충남", 60),
            Def("region_jeonbuk", "전라북도지사", "전라북도의 모든 시·군을 채웠어요", "region", "#33C08A", "flag", "region", "전북", 70),
            Def("region_jeonnam", "전라남도지사", "전라남도의 모든 시·군을 채웠어요", "region", "#1FA876", "flag", "region", "전남", 80),
            Def("region_gyeongbuk", "경상북도지사", "경상북도의 모든 시·군을 채웠어요", "region", "#A98BF0", "flag", "region", "경북", 90),
            Def("region_gyeongnam", "경상남도지사", "경상남도의 모든 시·군을 채웠어요", "region", "#8B6BE0", "flag", "region", "경남", 100),
            Def("region_jeju", "제주도지사", "제주의 모든 시·군을 채웠어요", "region", "#F47BB4", "flag", "region", "제주", 110),
            // ─── 기록(diary) — conditionValue = 작성 일기 임계값 ───
            Def("diary_1", "첫 발자국", "첫 일기를 남겼어요", "diary", "#7DC4A5", "edit_note", "diary", "1", 200),
            Def("diary_10", "기록의 시작", "일기 10개를 작성했어요", "diary", "#54B98A", "menu_book", "diary", "10", 210),
            Def("diary_30", "꾸준한 기록가", "일기 30개를 작성했어요", "diary", "#3FA9D6", "auto_stories", "diary", "30", 220),
            Def("diary_50", "기록 수집가", "일기 50개를 작성했어요", "diary", "#6E8BE0", "collections_bookmark", "diary", "50", 230),
            Def("diary_100", "기록 마스터", "일기 100개를 작성했어요", "diary", "#C9A23B", "workspace_premium", "diary", "100", 240),
            // ─── 모찌(character) — conditionValue = 모찌 레벨 ───
            Def("mochi_lv5", "모찌 새싹", "모찌를 Lv.5까지 키웠어요", "character", "#FFB0C0", "spa", "mochi_level", "5", 300),
            Def("mochi_lv10", "모찌 단짝", "모찌를 Lv.10까지 키웠어요", "character", "#F583A8", "favorite", "mochi_level", "10", 310),
            Def("mochi_lv15", "모찌 베테랑", "모찌를 Lv.15까지 키웠어요", "character", "#C78BE0", "military_tech", "mochi_level", "15", 320),
            Def("mochi_lv20", "모찌 마스터", "모찌를 만렙(Lv.20)까지 키웠어요", "character", "#B07BE0", "workspace_premium", "mochi_level", "20", 330)
        )
    }
}
