package com.mark.wsdeck.data

/** 對應 iOS 的 SearchQuery（§4.4.1：條件間 AND、同條件內 OR） */
data class SearchQuery(
    val keyword: String = "",
    val levels: Set<Int> = emptySet(),
    val colors: Set<CardColor> = emptySet(),
    val types: Set<CardType> = emptySet(),
    val triggers: Set<TriggerIcon> = emptySet(),
    val traits: Set<String> = emptySet(),
    /** 作品篩選；null = 全部 */
    val titleCode: String? = null,
    val ownership: OwnershipFilter = OwnershipFilter.ALL,
) {
    val hasActiveFilters: Boolean
        get() = levels.isNotEmpty() || colors.isNotEmpty() || types.isNotEmpty() ||
            triggers.isNotEmpty() || traits.isNotEmpty() || titleCode != null ||
            ownership != OwnershipFilter.ALL

    companion object {
        /** 卡號比對忽略大小寫與 `/` `-` */
        fun normalizeCardNumber(s: String) = s.replace("/", "").replace("-", "")
    }
}

/** 對應 iOS 的 OwnershipFilter——收藏狀態篩選，套用在 CardRepository.search() 的結果之上 */
enum class OwnershipFilter {
    ALL, OWNED, MISSING;

    val label: String
        get() = when (this) {
            ALL -> "全部"
            OWNED -> "已擁有"
            MISSING -> "未擁有"
        }
}
