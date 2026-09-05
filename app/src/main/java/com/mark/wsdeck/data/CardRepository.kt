package com.mark.wsdeck.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 圖鑑瀏覽單位：大多數作品跟 CardSetMeta 一對一，但同系列橫跨多個商品代碼
 * （如「葬送的芙莉蓮」有 S108/S128/S136 三波）的作品會拆成好幾個。
 * [id] 是篩選、收藏、導覽共用的識別碼——沒拆彈時就是 titleCode 本身，
 * 拆彈時是各自的 productCode（如 "SFN/S108"），兩者字串上可能重疊，
 * 一律靠 CardRepository.Snapshot.productCodes 這個集合判斷該用哪種比對方式。
 */
data class BrowsableSet(
    val id: String,
    val titleCode: String,
    val titleNameZH: String,
    val titleNameJP: String,
    val cardCount: Int,
    /** 原始商品代碼（如 "SFN/S108"），拆彈的作品才有值，圖鑑卡片右下角當輔助資訊顯示 */
    val productCode: String? = null,
    /** null＝沒拆彈，顯示原本作品名；有值＝顯示「作品名 標籤」。標籤優先用
     *  wave_names.json 提供的官方商品名稱（如「Vol.2」「新装版」），該系列
     *  官方資料還不夠乾淨時才退回舊的「第一彈/第二彈」數字猜測法 */
    val waveLabel: String? = null,
) {
    val displayNameZH: String get() = waveLabel?.let { "$titleNameZH $it" } ?: titleNameZH
}

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
        val browsableSets: List<BrowsableSet> = emptyList(),
        val productCodes: Set<String> = emptySet(),
        val titleByCardId: Map<String, String> = emptyMap(),
        val cardById: Map<String, Card> = emptyMap(),
        val allTraits: List<String> = emptyList(),
        val relationIndex: Map<String, List<CardRelation>> = emptyMap(),
    )

    /** 能力文字裡以「」指名的關聯卡片（羈絆／CX連動／被指名），對應 iOS CardDatabase.relations(for:) */
    fun relations(card: Card): List<CardRelation> = snapshot.relationIndex[card.id] ?: emptyList()

    var snapshot: Snapshot = Snapshot()
        private set

    var loadError: String? = null
        private set

    /** productCode → 官方彈次標籤；重建 browsableSets 時要用 */
    private var waveNameOverrides: Map<String, String> = emptyMap()

    /** 重新載入，更新卡表下載完之後呼叫（跟 load() 是同一套邏輯，只是取個對應 iOS reload() 的名字） */
    suspend fun reload(): Boolean = load(waveNameOverrides)

    /** [waveNameOverrides] 是啟動時已經有的官方彈次標籤快取（見 WaveNameRepository），
     *  這樣圖鑑一開始建立就是官方名稱，不用等網路查完才從數字猜測法換過來 */
    suspend fun load(waveNameOverrides: Map<String, String> = emptyMap()): Boolean = withContext(Dispatchers.Default) {
        this@CardRepository.waveNameOverrides = waveNameOverrides
        val assetNames = try {
            context.assets.list("")!!.filter { it.endsWith("_cards.json") }
        } catch (e: Exception) {
            loadError = "讀不到卡片資料檔：${e.message}"
            return@withContext false
        }
        // 下載更新的卡表放在 CardDataStore，檔名對得上就蓋過內建 assets 版本；
        // 還沒下載過的作品照舊用內建版本，兩邊合併而不是誰有就整組取代誰
        val downloadedDir = CardDataStore.directory(context)
        val downloadedNames = downloadedDir.listFiles { f -> f.name.endsWith("_cards.json") }
            ?.map { it.name } ?: emptyList()
        val names = (assetNames + downloadedNames).toSet().sorted()
        if (names.isEmpty()) {
            loadError = "找不到卡片資料檔（*_cards.json）"
            return@withContext false
        }

        val cards = mutableListOf<Card>()
        val sets = mutableListOf<CardSetMeta>()
        val titleByCardId = mutableMapOf<String, String>()
        for (name in names) {
            try {
                val downloaded = java.io.File(downloadedDir, name)
                val text = if (downloaded.exists()) {
                    downloaded.readText()
                } else {
                    context.assets.open(name).bufferedReader().use { it.readText() }
                }
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

        val sortedSets = sets.sortedBy { it.titleCode }
        val (browsableSets, productCodes) = buildBrowsableSets(sortedSets, cards, titleByCardId, waveNameOverrides)

        snapshot = Snapshot(
            cards = sortCards(cards, titleByCardId),
            sets = sortedSets,
            browsableSets = browsableSets,
            productCodes = productCodes,
            titleByCardId = titleByCardId,
            cardById = cardById,
            allTraits = cards.flatMap { it.traitsJP }.distinct().sorted(),
            relationIndex = buildCardRelations(cards),
        )
        loadError = null
        true
    }

    /** WaveNameRepository 背景抓到新版官方彈次標籤時呼叫，只重建 browsableSets，
     *  不用重新解一次整份卡表 JSON（對應 iOS CardDatabase.applyWaveNameOverrides） */
    fun applyWaveNameOverrides(overrides: Map<String, String>) {
        if (snapshot.cards.isEmpty()) return
        waveNameOverrides = overrides
        val (browsableSets, productCodes) =
            buildBrowsableSets(snapshot.sets, snapshot.cards, snapshot.titleByCardId, overrides)
        snapshot = snapshot.copy(browsableSets = browsableSets, productCodes = productCodes)
    }

    /** 同系列橫跨多個商品代碼的作品拆成好幾個瀏覽單位；只有 1 個代碼的作品
     *  維持原樣，用 titleCode 當 id（對應 iOS CardDatabase.buildBrowsableSets） */
    private fun buildBrowsableSets(
        sets: List<CardSetMeta>,
        cards: List<Card>,
        titleByCardId: Map<String, String>,
        waveNameOverrides: Map<String, String>,
    ): Pair<List<BrowsableSet>, Set<String>> {
        val cardsByTitle = cards.groupBy { titleByCardId[it.id] ?: "" }
        val result = mutableListOf<BrowsableSet>()
        val productCodes = mutableSetOf<String>()
        for (meta in sets) {
            val titleCards = cardsByTitle[meta.titleCode] ?: emptyList()
            val codes = titleCards.map { it.productCode }.toSet()
            if (codes.size <= 1) {
                result += BrowsableSet(
                    id = meta.titleCode, titleCode = meta.titleCode,
                    titleNameZH = meta.titleNameZH, titleNameJP = meta.titleNameJP,
                    cardCount = titleCards.size, productCode = null, waveLabel = null,
                )
            } else {
                // 依商品代碼裡的數字排序（如 S108 < S128 < S136）當顯示順序
                val ordered = codes.sortedWith(compareBy({ numericSuffix(it) }, { it }))
                // 官方標籤要整個系列每一彈都查得到才採用（見 make_wave_names.py
                // 的產生規則），免得同系列一部分用官方名稱、一部分用猜的
                val officialLabels: Map<String, String>? =
                    ordered.associateWith { waveNameOverrides[it] }
                        .takeIf { m -> m.values.all { it != null } }
                        ?.mapValues { it.value!! }
                ordered.forEachIndexed { index, code ->
                    val count = titleCards.count { it.productCode == code }
                    val label = if (officialLabels != null) {
                        officialLabels[code]?.takeIf { it.isNotEmpty() }
                    } else {
                        waveLabel(index + 1)
                    }
                    result += BrowsableSet(
                        id = code, titleCode = meta.titleCode,
                        titleNameZH = meta.titleNameZH, titleNameJP = meta.titleNameJP,
                        cardCount = count, productCode = code,
                        waveLabel = label,
                    )
                    productCodes += code
                }
            }
        }
        return result to productCodes
    }

    /** 商品代碼結尾的數字（如 "SFN/S108" → 108），沒有數字結尾就當 0 */
    private fun numericSuffix(code: String): Int =
        code.takeLastWhile { it.isDigit() }.toIntOrNull() ?: 0

    private fun waveLabel(index: Int): String {
        val ordinals = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
        return if (index in 1..ordinals.size) "第${ordinals[index - 1]}彈" else "第${index}彈"
    }

    /** 預設排序：作品 → 等級 → 顏色 → 卡號，CX 排最後（與 iOS 一致） */
    private fun sortCards(cards: List<Card>, titleByCardId: Map<String, String>) =
        cards.sortedWith(
            compareBy(
                { titleByCardId[it.id] ?: "" },
                { if (it.cardType == CardType.CLIMAX) 1 else 0 },
                { it.level ?: 99 },
                { it.color?.ordinal ?: 9 },
                { it.id },
            )
        )

    fun titleCode(card: Card): String? = snapshot.titleByCardId[card.id]

    /** id 有沒有拆過彈：拆過的作品，篩選/收藏/導覽用的 id 是商品代碼而不是
     *  titleCode，兩種字串都可能長得像（有些 titleCode 本身就帶斜線），
     *  靠 snapshot.productCodes 分辨，不要用字串形狀猜 */
    fun cards(inScope: String): List<Card> =
        if (inScope in snapshot.productCodes) {
            snapshot.cards.filter { it.productCode == inScope }
        } else {
            snapshot.cards.filter { snapshot.titleByCardId[it.id] == inScope }
        }

    /**
     * 只列這個瀏覽單位出現過的特徵，篩選頁鎖定作品/商品時用，對應 iOS 的
     * CardDatabase.traits(inScope:)——全部特徵一次列出來常常有上百個跨作品的
     * 標籤，鎖定範圍後大多數根本不會出現在結果裡，縮小範圍才看得出「這裡
     * 有哪些特徵可以篩」。
     */
    fun traits(inScope: String): List<String> =
        cards(inScope).flatMap { it.traitsJP }.distinct().sorted()

    fun search(query: SearchQuery): List<Card> {
        val keyword = query.keyword.trim().lowercase()
        val normalized = SearchQuery.normalizeCardNumber(keyword)
        return snapshot.cards.filter { card ->
            val scope = query.titleCode
            if (scope != null) {
                val matches = if (scope in snapshot.productCodes) {
                    card.productCode == scope
                } else {
                    snapshot.titleByCardId[card.id] == scope
                }
                if (!matches) return@filter false
            }
            if (query.levels.isNotEmpty() && card.level !in query.levels) return@filter false
            if (query.colors.isNotEmpty() && (card.color == null || card.color !in query.colors)) return@filter false
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
