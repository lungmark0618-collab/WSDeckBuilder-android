package com.mark.wsdeck.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 對應 iOS 的 Deck（SwiftData）。同樣的設計：entries 只存刷版卡號與張數，
 * 不存卡片內容——換卡表、修譯文都不需要遷移既有牌組。
 */
@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val note: String = "",
    /** 封面卡的刷版卡號；空字串表示自動取牌組中等級最高的一張 */
    val coverPrintingId: String = "",
    /** 使用者在卡表拖曳排序過的卡片 id（不分刷版），逗號分隔，保留順序。
     *  沒被拖曳過的卡不在這裡，顯示時自然照卡號排序接在後面 */
    val cardOrder: String = "",
)

/** 一筆 = 某張卡的某個刷版放了幾張 */
@Entity(
    tableName = "deck_entries",
    foreignKeys = [ForeignKey(
        entity = DeckEntity::class,
        parentColumns = ["uuid"],
        childColumns = ["deckUuid"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("deckUuid")],
)
data class DeckEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckUuid: String,
    val printingId: String,
    val count: Int,
)

data class DeckWithEntries(
    @Embedded val deck: DeckEntity,
    @Relation(parentColumn = "uuid", entityColumn = "deckUuid")
    val entries: List<DeckEntryEntity>,
) {
    val totalCount: Int get() = entries.sumOf { it.count }
}

@Dao
interface DeckDao {
    @Transaction
    @Query("SELECT * FROM decks ORDER BY createdAt")
    fun observeDecks(): Flow<List<DeckWithEntries>>

    @Transaction
    @Query("SELECT * FROM decks WHERE uuid = :uuid")
    fun observeDeck(uuid: String): Flow<DeckWithEntries?>

    @Insert
    suspend fun insertDeck(deck: DeckEntity)

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Query("DELETE FROM decks WHERE uuid = :uuid")
    suspend fun deleteDeck(uuid: String)

    @Query("SELECT * FROM deck_entries WHERE deckUuid = :deckUuid AND printingId = :printingId LIMIT 1")
    suspend fun findEntry(deckUuid: String, printingId: String): DeckEntryEntity?

    @Insert
    suspend fun insertEntry(entry: DeckEntryEntity)

    @Update
    suspend fun updateEntry(entry: DeckEntryEntity)

    @Query("DELETE FROM deck_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("DELETE FROM deck_entries WHERE deckUuid = :deckUuid AND printingId = :printingId")
    suspend fun deleteEntryByPrinting(deckUuid: String, printingId: String)

    @Query("UPDATE decks SET updatedAt = :timestamp WHERE uuid = :uuid")
    suspend fun touch(uuid: String, timestamp: Long)
}

@Database(
    entities = [DeckEntity::class, DeckEntryEntity::class, CollectionEntryEntity::class],
    version = 3,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun collectionDao(): CollectionDao
}

/** 加入拖曳排序記錄欄位，用真的 migration 保留使用者既有牌組——
 *  不能像 1→2 那次直接讓 fallbackToDestructiveMigration 整個清掉重建 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE decks ADD COLUMN cardOrder TEXT NOT NULL DEFAULT ''")
    }
}
