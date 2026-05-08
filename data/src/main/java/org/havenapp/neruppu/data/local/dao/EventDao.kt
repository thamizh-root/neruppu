package org.havenapp.neruppu.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.havenapp.neruppu.data.local.entity.EventEntity

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getEventsPaging(): PagingSource<Int, EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): EventEntity?

    @androidx.room.Update
    suspend fun updateEvent(event: EventEntity)

    @Query("DELETE FROM events")
    suspend fun clearEvents()
}
