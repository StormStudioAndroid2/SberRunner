package xyz.fcr.sberrunner.data.room

import androidx.lifecycle.LiveData
import androidx.room.*
import xyz.fcr.sberrunner.data.model.Run
import xyz.fcr.sberrunner.utils.Constants.DB_NAME

@Dao
interface RunDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addRun(run: Run)

    @Delete
    fun deleteRun(run: Run)

    @Query("SELECT * FROM $DB_NAME ORDER BY timestamp DESC")
    fun getAllRuns(): List<Run>

    @Query("SELECT * FROM $DB_NAME WHERE id = :runId")
    fun getRun(runId: Int): LiveData<Run>
}