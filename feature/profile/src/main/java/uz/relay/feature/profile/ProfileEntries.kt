package uz.relay.feature.profile

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import uz.relay.core.navigation.key.EditProfileKey
import uz.relay.core.navigation.key.MyProfileKey
import uz.relay.core.navigation.key.UserProfileKey
import uz.relay.feature.profile.edit.EditProfileScreen
import uz.relay.feature.profile.me.MyProfileScreen
import uz.relay.feature.profile.user.UserProfileScreen

/** Bu feature ekranlarini ilovaning NavDisplay'iga ro'yxatdan o'tkazadi (kalit → ekran). */
fun EntryProviderScope<NavKey>.profileEntries() {
    entry<MyProfileKey> { MyProfileScreen() }
    entry<EditProfileKey> { EditProfileScreen() }
    entry<UserProfileKey> { key -> UserProfileScreen(userId = key.userId) }
}
