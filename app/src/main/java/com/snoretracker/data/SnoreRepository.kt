package com.snoretracker.data

import kotlinx.coroutines.flow.Flow

class SnoreRepository(private val snoreDao: SnoreDao) {

    val allSessions: Flow<List<SnoreSession>> = snoreDao.getAllSessions()

    suspend fun saveSession(session: SnoreSession) {
        snoreDao.insertSession(session)
    }

    suspend fun deleteSession(session: SnoreSession) {
        snoreDao.deleteSession(session)
    }

    suspend fun deleteAllSessions() {
        snoreDao.deleteAllSessions()
    }
    
    suspend fun getSessionById(id: Long): SnoreSession? {
        return snoreDao.getSessionById(id)
    }
}
