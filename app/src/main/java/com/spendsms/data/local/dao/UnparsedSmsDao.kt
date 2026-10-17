package com.spendsms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendsms.data.local.entity.UnparsedSmsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnparsedSmsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnparsedSms(sms: UnparsedSmsEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnparsedSmsList(list: List<UnparsedSmsEntity>): List<Long>

    @Query("SELECT * FROM unparsed_sms WHERE is_ignored = 0 ORDER BY timestamp DESC")
    fun getPendingReviewSms(): Flow<List<UnparsedSmsEntity>>

    @Query("UPDATE unparsed_sms SET is_ignored = 1 WHERE id = :id")
    suspend fun ignoreSms(id: Long)

    @Query("DELETE FROM unparsed_sms WHERE id = :id")
    suspend fun deleteUnparsedSms(id: Long)

    @Query("DELETE FROM unparsed_sms")
    suspend fun deleteAllUnparsedSms()

    @Query("SELECT COUNT(*) > 0 FROM unparsed_sms WHERE sms_id = :smsId AND timestamp = :timestamp")
    suspend fun exists(smsId: Long, timestamp: Long): Boolean

    @Query("SELECT COUNT(*) FROM unparsed_sms WHERE is_ignored = 0")
    fun getPendingCount(): Flow<Int>
}
