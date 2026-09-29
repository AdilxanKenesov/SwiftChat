package uz.relay.domain.model

enum class AuthState {
    LOGGED_OUT,

    /** Logged in, but a new user has not filled in the profile (name, username) yet. */
    NEEDS_PROFILE,
    LOGGED_IN
}
