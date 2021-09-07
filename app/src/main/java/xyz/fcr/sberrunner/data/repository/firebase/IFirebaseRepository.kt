package xyz.fcr.sberrunner.data.repository.firebase

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult

/**
 * Интерфейс взаимодействия с объектом FirebaseAuth
 */
interface IFirebaseRepository {

    /**
     * Регистрация пользователя
     *
     * @param name [String] - имя пользователя
     * @param email [String] - email пользователя
     * @param password [String] - пароль пользователя
     * @param weight [String] - вес пользователя
     *
     * @return [Task] - асинхронный результат выполенения регистрации
     */
    fun registration(
        name: String,
        email: String,
        password: String,
        weight: String
    ): Task<AuthResult>

    /**
     * Вход в аккаунт
     *
     * @param email [String] - email пользователя
     * @param password [String] - пароль пользователя
     *
     * @return [Task] - результат асинхронного запроса входа в аккаунт
     */
    fun login(email: String, password: String): Task<AuthResult>

    /**
     * Отправка сообщения на email пользователя со сбросом пароля
     *
     * @param email [String] - email пользователя
     *
     * @return [Task] - результат асинхронного запроса сброса
     */
    fun sendResetEmail(email: String): Task<Void>

    /**
     * Выход пользователя из аккаунта
     */
    fun signOut()

    /**
     * Удаление пользователем своего аккаунта
     *
     * @return [Task] - результат асинхронного запроса удаления аккаунта
     */
    fun deleteAccount(): Task<Void>
}