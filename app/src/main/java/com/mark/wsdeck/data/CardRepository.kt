package com.mark.wsdeck.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 卡表載入與搜尋。對應 iOS 的 CardDatabase。
 *
 * 解 5.7 MB 的 JSON 在主執行緒做會卡住畫面數秒，所以 load() 是 suspend，
 * 實際工作丟到 Dispatchers.Default。
 */
class CardRepository(private val context: Context) {

    data class Snapshot(
        val cards: List<Card> = emptyList(),
        val sets: List<CardSetMeta> = emptyList(),
        val titleByCardId: Map<String, String> = emptyMap(),
        val cardById: Map<String, Card> = emptyMap(),
        val allTraits: List<String> = emptyList(),
    )

    var snapshot: Snapshot = Snapshot()
        private set

    var loadError: String? = null
        private set

    suspend fun load(): Boolean = withContext(Dispatchers.Default) {
        val names = try {
            context.assets.list("")!!.filter { it.endsWith("_cards.json") }.sorted()
        } catch (e: Exception) {
            loadError = "讀不到卡片資料檔：${e.message}"
            return@withContext false
        }
        if (names.isEmpty()) {
            loadError = "找不到卡片資料檔（*_cards.json）"
            return@withContext false
        }

        val cards = mutableListOf<Card>()
        val sets = mutableListOf<CardSetMeta>()
        val titleByCardId = mutableMapOf<String, String>()
        for (name in names) {
            try {
                val text = context.assets.open(name).bufferedReader().use { it.readText() }
                val set = cardJson.decodeFromString<CardSet>(text)
                sets += set.meta
                for (c in set.cards) titleByCardId[c.id] = set.meta.titleCode
                cards += set.cards
            } catch (e: Exception) {
                loadError = "$name 載入失敗：${e.message}"
                return@withContext false
            }
        }

        // 任一刷版卡號都要能查到卡片；SP 特典卡沒有同號普卡，所以基礎卡號也建索引
        val cardById = mutableMapOf<String, Card>()
        for (c in cards) {
            cardById[c.id] = c
            for (p in c.printings) cardById[p.id] = c
        }

        snapshot = Snapshot(
            cards = sortCards(cards, titleByCardId),
            sets = sets.sortedBy { it.titleCode },
            titleByCardId = titleByCardId,
            cardById = cardById,
            allTraits = cards.flatMap { it.traitsJP }.distinct().sorted(),
        )
        loadError = null
        true
    }

    /** 預設排序：作品 → 等級 → 顏色 → 卡號，CX 排最後（與 iOS 一致） */
    private fun sortCards(cards: List<Card>, titleByCardId: Map<String, String>) =
        cards.sortedWith(
            compareBy(
                { titleByCardId[it.id] ?: "" },
                { if (it.cardType == CardType.CLIMAX) 1 else 0 },
                { it.level ?: 99 },
                { it.color.ordinal },
                { it.id },
            )
        )

    fun titleCode(card: Card): String? = snapshot.titleByCardId[card.id]

    fun search(query: SearchQuery): List<Card> {
        val keyword = query.keyword.trim().lowercase()
        val normalized = SearchQuery.normalizeCardNumber(keyword)
        return snapshot.cards.filter { card ->
            if (query.titleCode != null &&
                snapshot.titleByCardId[card.id] != query.titleCode) return@filter false
            if (query.levels.isNotEmpty() && card.level !in query.levels) return@filter false
            if (query.colors.isNotEmpty() && card.color !in query.colors) return@filter false
            if (query.types.isNotEmpty() && card.cardType !in query.types) return@filter false
            if (query.triggers.isNotEmpty() && card.trigger !in query.triggers) return@filter false
            if (query.traits.isNotEmpty() &&
                query.traits.none { it in card.traitsJP }) return@filter false
            if (keyword.isEmpty()) return@filter true

            // 卡號比對忽略大小寫與 / -（打 w139075 也要命中 BRD/W139-075）
            if (normalized.isNotEmpty() &&
                card.printings.any {
                    SearchQuery.normalizeCardNumber(it.id.lowercase()).contains(normalized)
                }
            ) return@filter true
            card.searchBlob.contains(keyword)
        }
    }
}
