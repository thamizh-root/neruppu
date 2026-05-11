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
    @Query("SELECT * FROM events WHERE (:filter = 'All' OR sensorType = :filter) ORDER BY timestamp DESC")
    fun getEventsPaging(filter: String): PagingSource<Int, EventEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: EventEntity): Long

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): EventEntity?

    @androidx.room.Update
    suspend fun updateEvent(event: EventEntity)

    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<EventEntity>

    @Query("SELECT * FROM events LIMIT :pageSize OFFSET :offset")
    suspend fun getEventsPage(offset: Int, pageSize: Int): List<EventEntity>

    @Query("DELETE FROM events")
    suspend fun clearEvents()
}
