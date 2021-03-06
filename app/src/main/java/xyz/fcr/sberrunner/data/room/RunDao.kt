package xyz.fcr.sberrunner.data.room

import androidx.room.*
import xyz.fcr.sberrunner.data.model.RunEntity
import xyz.fcr.sberrunner.utils.Constants.DB_NAME

/**
 * Интерфейс взаимодействия базой данных бега
 */
@Dao
interface RunDao {

    /**
     * Добавление результатов одного забега
     *
     * @param run [RunEntity] - забег пользователя
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addRun(run: RunEntity)

    /**
     * Удаление выбранного забега
     *
     * @param run [RunEntity] - забег пользователя
     */
    @Delete
    fun deleteRun(run: RunEntity)

    /**
     * Удаление всех забегов из базы данных
     */
    @Query("DELETE FROM $DB_NAME")
    fun clearRuns()

    /**
     * Получение всех забегов из базы данных
     *
     * @return [List] - список забегов пользователя
     */
    @Query("SELECT * FROM $DB_NAME ORDER BY timestamp DESC")
    fun getAllRuns(): List<RunEntity>

    /**
     * Получение забега из базы данных
     *
     * @param timestamp [Long] - временная отметка забега
     *
     * @return [RunEntity] - список забегов пользователя
     */
    @Query("SELECT * FROM $DB_NAME WHERE timestamp = :timestamp")
    fun getRun(timestamp: Long): RunEntity

    /**
     * Переключение флагов на удаление (при синхронизации забеги будут удалены)
     *
     * @param timestamp [Long] - временная отметка забега
     * @param toDelete [Boolean] - флаг на удаление
     *
     * @return [List] - список забегов пользователя
     */
    @Query("UPDATE $DB_NAME SET toDeleteFlag = :toDelete WHERE timestamp = :timestamp")
    fun switchToDeleteFlag(timestamp: Long, toDelete: Boolean)

    /**
     * Удаление всех забегов с флагом на удаление
     */
    @Query("DELETE FROM $DB_NAME WHERE toDeleteFlag = 1")
    fun removerMarkedToDelete()

    /**
     * Удаление забегов из выбранного списка временных отметок
     *
     * @param timeStampList [List] - список временных отметок забегов
     */
    @Query("DELETE FROM $DB_NAME WHERE timestamp in (:timeStampList)")
    fun removeRuns(timeStampList: List<Long>)
}