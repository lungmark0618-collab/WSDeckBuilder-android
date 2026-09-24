package com.mark.wsdeck.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

/**
 * 「我的收藏」的 Room 存取，對應 iOS CollectionStore 裡跟 ModelContext 有關的那半。
 * 純邏輯（依卡片彙總、算缺卡）另外放在 CollectionStore.kt，方便單元測試。
 */
class CollectionRepository(context: Context) {
    // 理由同 DeckRepository：把「允許整個清掉重建」限定在版本 1 這個已知起點，
    // 之後漏寫 migration 會直接丟例外，不會悄悄把使用者資料清空
    private val db = Room.databaseBuilder(
        context.applicationContext, AppDatabase::class.java, "wsdeck.db",
    ).addMigrations(MIGRATION_2_3).fallbackToDestructiveMigrationFrom(true, 1).build()
    private val dao = db.collectionDao()

    fun observeAll(): Flow<List<CollectionEntryEntity>> = dao.observeAll()

    /** 調整某刷版的擁有張數；歸零即刪除該筆（跟 DeckRepository.adjust 同樣的規則） */
    suspend fun adjust(printingId: String, delta: Int) {
        val existing = dao.find(printingId)
        if (existing != null) {
            val newCount = (existing.ownedCount + delta).coerceAtLeast(0)
            if (newCount == 0) {
                dao.delete(printingId)
            } else {
                dao.update(existing.copy(ownedCount = newCount, updatedAt = System.currentTimeMillis()))
            }
        } else if (delta > 0) {
            dao.insert(CollectionEntryEntity(printingId = printingId, ownedCount = delta))
        }
    }

    /** 缺卡頁「一次收齊」用：直接補到需要的張數 */
    suspend fun fill(printingId: String, owned: Int, needed: Int) {
        if (needed > owned) adjust(printingId, needed - owned)
    }
}
