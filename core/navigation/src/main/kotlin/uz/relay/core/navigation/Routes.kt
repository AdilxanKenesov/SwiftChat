package uz.relay.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data object AuthRoute

@Serializable
data object ChatsRoute

@Serializable
data class ConversationRoute(val chatId: String)

@Serializable
data class GroupRoute(val groupId: String)

@Serializable
data class ProfileRoute(val userId: String)
