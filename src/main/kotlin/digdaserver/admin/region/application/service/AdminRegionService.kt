package digdaserver.admin.region.application.service

interface AdminRegionService {

    /** 그룹에서 어드민이 채워둔 region_key 목록. */
    fun filled(groupRoomId: Long): List<String>

    /** 지역 채움(멱등). 채움 후 전체 채운 목록 반환. */
    fun fill(groupRoomId: Long, regionKeys: List<String>): List<String>

    /** 지정 지역 채움 해제. 해제 후 전체 채운 목록 반환. */
    fun unfill(groupRoomId: Long, regionKeys: List<String>): List<String>

    /** 그룹의 모든 채움 해제. */
    fun clear(groupRoomId: Long)
}
