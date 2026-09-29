package uz.relay.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import uz.relay.core.navigation.AppNavigationHandler
import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.key.SplashKey
import uz.relay.feature.auth.authEntries
import uz.relay.feature.chats.chatsEntries
import uz.relay.feature.conversation.conversationEntries
import uz.relay.feature.group.groupEntries
import uz.relay.feature.profile.profileEntries

/**
 * Ilovaning yagona back stack'i (Navigation 3): har bir Directions buyrug'i [AppNavigationHandler] orqali
 * shu yerga keladi va stekka qo'llanadi. Ekranlar har bir feature'ning `...Entries()` funksiyasida
 * ro'yxatdan o'tadi — app moduli faqat ularni yig'adi.
 */
@Composable
fun AppNavHost(navigationHandler: AppNavigationHandler) {
    // Kalitlar @Serializable: stek jarayon o'ldirilganda (process death) ham saqlanib, qayta tiklanadi.
    val backStack = rememberNavBackStack(SplashKey)

    LaunchedEffect(navigationHandler) {
        navigationHandler.params.collect { param -> backStack.apply(param) }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.pop() },
        entryDecorators = listOf(
            // Har bir ekran uchun alohida rememberSaveable holati.
            rememberSaveableStateHolderNavEntryDecorator(),
            // Har bir ekran uchun alohida ViewModelStore: ekran stekdan chiqsa, uning ViewModel'i ham tozalanadi.
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            authEntries()
            chatsEntries()
            conversationEntries()
            groupEntries()
            profileEntries()
        }
    )
}

/** Oxirgi ekran hech qachon olib tashlanmaydi: bo'sh stek NavDisplay'ni yiqitadi. */
private fun MutableList<NavKey>.pop() {
    if (size > 1) removeAt(lastIndex)
}

/** [AppNavigationParam] buyrug'ini stekka qo'llaydi (sof funksiya — alohida test qilish oson). */
internal fun MutableList<NavKey>.apply(param: AppNavigationParam) {
    when (param) {
        is AppNavigationParam.To -> if (!(param.singleTop && lastOrNull() == param.key)) add(param.key)
        is AppNavigationParam.Replace -> {
            if (isNotEmpty()) removeAt(lastIndex)
            add(param.key)
        }

        AppNavigationParam.Back -> pop()
        is AppNavigationParam.BackTo -> {
            // Stekda yo'q: hamma ekranni yopib yuborgandan ko'ra, hech narsa qilmaymiz.
            val index = lastIndexOf(param.key)
            if (index >= 0) {
                val keep = if (param.inclusive) index else index + 1
                while (size > keep.coerceAtLeast(1)) removeAt(lastIndex)
            }
        }

        is AppNavigationParam.BackToOrTo -> {
            val index = lastIndexOf(param.key)
            if (index >= 0) while (size > index + 1) removeAt(lastIndex) else add(param.key)
        }

        is AppNavigationParam.ResetTo -> {
            // Allaqachon faqat shu ekran bo'lsa — qayta yaratmaymiz (holat saqlanadi).
            if (size == 1 && first() == param.key) return
            clear()
            add(param.key)
        }
    }
}
