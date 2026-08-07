package com.mark.wsdeck.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 我的收藏：某個刷版實際擁有幾張，對應 iOS 的 CollectionEntry */
@Entity(tableName = "collection_entries")
data class CollectionEntryEntity(
    @PrimaryKey val printingId: String,
    val ownedCount: Int,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collection_entries")
    fun observeAll(): Flow<List<CollectionEntryEntity>>

    @Query("SELECT * FROM collection_entries WHERE printingId = :printingId LIMIT 1")
    suspend fun find(printingId: String): CollectionEntryEntity?

    @Insert
    suspend fun insert(entry: CollectionEntryEntity)

    @Update
    suspend fun update(entry: CollectionEntryEntity)

    @Query("DELETE FROM collection_entries WHERE printingId = :printingId")
    suspend fun delete(printingId: String)
}
