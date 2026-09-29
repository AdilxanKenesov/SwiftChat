package uz.relay.domain.model

/** Same rules as the server's UpdateMeRequest schema, so an invalid request is never sent. */
object ProfileRules {
    val USERNAME = Regex("^[a-zA-Z0-9_]{3,32}$")
    const val NAME_MAX = 128
    const val USERNAME_MAX = 32

    fun isNameValid(name: String) = name.trim().length in 1..NAME_MAX
    fun isUsernameValid(username: String) = USERNAME.matches(username)
}
