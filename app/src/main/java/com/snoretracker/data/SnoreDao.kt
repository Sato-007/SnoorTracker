package com.snoretracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SnoreSession)

    @Delete
    suspend fun deleteSession(session: SnoreSession)

    @Query("DELETE FROM snore_session")
    suspend fun deleteAllSessions()

    @Query("SELECT * FROM snore_session ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SnoreSession>>
    
    @Query("SELECT * FROM snore_session WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): SnoreSession?
}
