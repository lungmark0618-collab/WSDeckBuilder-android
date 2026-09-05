package com.mark.wsdeck.data

/** 卡片之間的指名關聯：能力文字裡以「」提到的卡，對應 iOS 的 CardRelation */
data class CardRelation(val card: Card, val kind: Kind) {
    val id: String get() = card.id + kind.name

    enum class Kind {
        CX_COMBO,       // 【CX連動】指定的 CX
        BOND,           // 絆／羈絆的對象
        CHANGE,         // 變身（チェンジ）的對象
        NAMED,          // 其他以卡名指名的卡
        REFERENCED_BY;  // 反向：這張卡被誰指名

        val label: String
            get() = when (this) {
                CX_COMBO -> "CX連動"
                BOND -> "羈絆"
                CHANGE -> "變身"
                NAMED -> "指名"
                REFERENCED_BY -> "被指名"
            }

        /** 數字越小越具體；同一張卡被多行提到時取最具體的 */
        val order: Int
            get() = when (this) {
                BOND -> 0
                CX_COMBO -> 1
                CHANGE -> 2
                NAMED -> 3
                REFERENCED_BY -> 4
            }

        companion object {
            /** 依該行文字的上下文判斷關聯種類 */
            fun of(line: String, target: Card): Kind = when {
                line.contains("絆") -> BOND
                line.contains("チェンジ") || line.contains("變身") -> CHANGE
                target.cardType == CardType.CLIMAX ||
                    line.contains("CXコンボ") || line.contains("CX連動") || line.contains("CX置場") -> CX_COMBO
                else -> NAMED
            }
        }
    }
}

/**
 * 能力文字中以「」指名的卡片（羈絆對象、CX 連動指定的 CX 等），同時建立反向關聯
 * （這張 CX 被哪些角色連動），對應 iOS CardDatabase.buildRelations。
 */
fun buildCardRelations(cards: List<Card>): Map<String, List<CardRelation>> {
    val byNameJp = cards.groupBy { it.nameJP }
    val index = mutableMapOf<String, MutableList<CardRelation>>()

    for (card in cards) {
        if (card.textJP.isEmpty()) continue
        for (line in card.textLinesJP) {
            for (name in quotedNames(line)) {
                if (name == card.nameJP) continue
                val targets = byNameJp[name] ?: continue
                val kind = CardRelation.Kind.of(line, targets[0])
                for (target in targets) {
                    if (target.id == card.id) continue
                    index.getOrPut(card.id) { mutableListOf() }.add(CardRelation(target, kind))
                    index.getOrPut(target.id) { mutableListOf() }
                        .add(CardRelation(card, CardRelation.Kind.REFERENCED_BY))
                }
            }
        }
    }

    // 同一張卡可能被多行提到：每張只留最具體的關聯（羈絆 > CX連動 > 變身 > 指名）
    return index.mapValues { (_, relations) ->
        val best = linkedMapOf<String, CardRelation>()
        for (relation in relations) {
            val existing = best[relation.card.id]
            if (existing == null || relation.kind.order < existing.kind.order) {
                best[relation.card.id] = relation
            }
        }
        best.values.sortedWith(compareBy({ it.kind.order }, { it.card.id }))
    }
}

private fun quotedNames(line: String): List<String> {
    val names = mutableListOf<String>()
    var current: StringBuilder? = null
    for (ch in line) {
        when (ch) {
            '「' -> current = StringBuilder()
            '」' -> {
                current?.let { if (it.isNotEmpty()) names += it.toString() }
                current = null
            }
            else -> current?.append(ch)
        }
    }
    return names
}
