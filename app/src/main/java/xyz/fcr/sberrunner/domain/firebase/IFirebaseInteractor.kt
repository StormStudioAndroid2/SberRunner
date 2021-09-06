package xyz.fcr.sberrunner.domain.firebase

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import io.reactivex.rxjava3.core.Single
import xyz.fcr.sberrunner.data.model.RunEntity

interface IFirebaseInteractor {

    fun login(email: String, password: String): Single<Task<AuthResult>>
    fun registration(name: String, email: String, pass: String, weight: String): Single<Task<AuthResult>>
    fun sendResetEmail(email: String): Single<Task<Void>>

    fun signOut(): Single<Unit>
    fun deleteAccount(): Single<Task<Void>>

    fun getDocumentFirestore(): Single<Task<DocumentSnapshot>>
    fun updateWeight(weight: String): Single<Task<Void>>
    fun updateName(name: String): Single<Task<Void>>

    /**
     * Метод получения объектов бега из Firestore
     *
     * @return [Single<List<Run>>] - объекты бега
     */
    fun getRunsDocumentsFromCloud(): Single<Task<QuerySnapshot>>
    fun fillUserDataInFirestore(name: String, weight: String): Single<Task<Void>>
}