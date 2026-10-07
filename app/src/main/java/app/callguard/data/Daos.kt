package app.callguard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules")
    suspend fun getAll(): List<RuleEntity>

    @Insert
    suspend fun insert(rule: RuleEntity): Long

    @Update
    suspend fun update(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 200")
    fun observeRecent(): Flow<List<HistoryEntity>>

    @Query("SELECT COUNT(*) FROM history WHERE timestamp >= :since")
    fun observeCountSince(since: Long): Flow<Int>

    @Insert
    suspend fun insert(item: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history WHERE timestamp < :before")
    suspend fun pruneBefore(before: Long)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface AllowedDao {
    @Query("SELECT * FROM allowed ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<AllowedEntity>>

    @Query("SELECT * FROM allowed")
    suspend fun getAll(): List<AllowedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AllowedEntity)

    @Query("DELETE FROM allowed WHERE number = :number")
    suspend fun delete(number: String)
}
