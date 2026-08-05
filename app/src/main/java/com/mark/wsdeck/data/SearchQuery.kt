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
) {
    val hasActiveFilters: Boolean
        get() = levels.isNotEmpty() || colors.isNotEmpty() || types.isNotEmpty() ||
            triggers.isNotEmpty() || traits.isNotEmpty() || titleCode != null

    companion object {
        /** 卡號比對忽略大小寫與 `/` `-` */
        fun normalizeCardNumber(s: String) = s.replace("/", "").replace("-", "")
    }
}
