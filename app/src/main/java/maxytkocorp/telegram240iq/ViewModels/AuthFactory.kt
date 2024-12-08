package maxytkocorp.telegram240iq.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import maxytkocorp.telegram240iq.Web.SessionManager

class AuthFactory(
    private val sessionManager: SessionManager,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(Auth::class.java) -> {
                Auth(sessionManager) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
