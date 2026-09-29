package uz.relay.feature.profile.user

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.ChatKey
import javax.inject.Inject

/**
 * [UserProfileContract.Directions] implementatsiyasi. `BackToOrTo` ishlatiladi: chat → profil → "Xabar" bosilsa
 * stekka ikkinchi bir xil chat qo'shilmaydi, mavjud chatga qaytiladi.
 */
internal class UserProfileDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : UserProfileContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)

    override suspend fun navigateToChat(chatId: String) = navigator.navigate(AppNavigationParam.BackToOrTo(ChatKey(chatId)))
}
