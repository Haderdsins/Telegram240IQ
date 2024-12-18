package maxytkocorp.telegram240iq.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import maxytkocorp.telegram240iq.ChatRepository
import maxytkocorp.telegram240iq.Web.SessionManager

class MainFactory(
    private val sessionManager: SessionManager,
    private val chatRepository: ChatRepository,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(Main::class.java)) {
            Main(sessionManager, chatRepository) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.simpleName}")
        }
    }
}
