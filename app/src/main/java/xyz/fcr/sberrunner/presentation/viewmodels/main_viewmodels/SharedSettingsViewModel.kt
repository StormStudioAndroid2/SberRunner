package xyz.fcr.sberrunner.presentation.viewmodels.main_viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import xyz.fcr.sberrunner.data.repository.FirebaseRepository
import xyz.fcr.sberrunner.utils.SchedulersProviderInterface
import javax.inject.Inject

class SharedSettingsViewModel @Inject constructor(
    val firebaseRepo: FirebaseRepository,
    val schedulersProvider: SchedulersProviderInterface
) : ViewModel() {

    private val _progressLiveData = MutableLiveData<Boolean>()
    private val _signOutLiveData = MutableLiveData<Boolean>()
    private val _deleteLiveData = MutableLiveData<Boolean>()

    private var disDeleteAccount: Disposable? = null

    fun exitAccount() {
        firebaseRepo.signOut()
        _signOutLiveData.postValue(true)
    }

    fun deleteAccount() {
        disDeleteAccount = Single.fromCallable { firebaseRepo.deleteAccount() }
            .doOnSubscribe { _progressLiveData.postValue(true) }
            .subscribeOn(schedulersProvider.io())
            .observeOn(schedulersProvider.ui())
            .subscribe { task ->
                task?.addOnCompleteListener {
                    when {
                        it.isSuccessful -> _deleteLiveData.postValue(true)
                        else -> _deleteLiveData.postValue(false)
                    }

                    _progressLiveData.postValue(false)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()

        disDeleteAccount?.dispose()
        disDeleteAccount = null
    }

    val progressLiveData: LiveData<Boolean>
        get() = _progressLiveData
    val signOutLiveData: LiveData<Boolean>
        get() = _signOutLiveData
    val deleteLiveData: LiveData<Boolean>
        get() = _deleteLiveData
}