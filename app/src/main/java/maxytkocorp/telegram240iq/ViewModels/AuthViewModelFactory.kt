package maxytkocorp.telegram240iq.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import maxytkocorp.telegram240iq.Web.SessionManager

class AuthViewModelFactory(
    private val sessionManager: SessionManager,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(sessionManager) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
