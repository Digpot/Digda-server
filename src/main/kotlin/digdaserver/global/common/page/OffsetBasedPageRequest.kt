package digdaserver.global.common.page

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

/**
 * Pageable implementation that supports arbitrary offset (not limited to multiples of page size).
 * This avoids data loss when offset is not a multiple of limit.
 */
class OffsetBasedPageRequest(
    private val offset: Long,
    private val limit: Int,
    private val sort: Sort = Sort.unsorted()
) : Pageable {

    init {
        require(offset >= 0) { "Offset must not be less than zero" }
        require(limit >= 1) { "Limit must not be less than one" }
    }

    override fun getPageNumber(): Int = (offset / limit).toInt()

    override fun getPageSize(): Int = limit

    override fun getOffset(): Long = offset

    override fun getSort(): Sort = sort

    override fun next(): Pageable = OffsetBasedPageRequest(offset + limit, limit, sort)

    override fun previousOrFirst(): Pageable =
        if (hasPrevious()) { OffsetBasedPageRequest((offset - limit).coerceAtLeast(0), limit, sort) } else { first() }

    override fun first(): Pageable = OffsetBasedPageRequest(0, limit, sort)

    override fun withPage(pageNumber: Int): Pageable =
        OffsetBasedPageRequest(pageNumber.toLong() * limit, limit, sort)

    override fun hasPrevious(): Boolean = offset > 0

    companion object {
        fun of(offset: Int, limit: Int, sort: Sort = Sort.unsorted()): OffsetBasedPageRequest =
            OffsetBasedPageRequest(offset.toLong(), limit, sort)
    }
}
