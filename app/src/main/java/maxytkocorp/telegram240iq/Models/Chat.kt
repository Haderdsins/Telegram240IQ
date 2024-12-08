package maxytkocorp.telegram240iq.Models

data class Chat(val name: String, val isChannel: Boolean, var lastKnownId: Int = Int.MAX_VALUE)
