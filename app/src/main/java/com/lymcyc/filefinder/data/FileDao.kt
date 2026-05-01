package com.lymcyc.filefinder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FileEntity>)

    @Query("DELETE FROM files")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM files")
    fun countFlow(): Flow<Int>

    @Query("""
        SELECT * FROM files
        WHERE nameLower  LIKE :q
           OR pinyinFull LIKE :q
           OR pinyinHead LIKE :q
        ORDER BY isDir DESC, modified DESC
        LIMIT 200
    """)
    suspend fun search(q: String): List<FileEntity>
}
