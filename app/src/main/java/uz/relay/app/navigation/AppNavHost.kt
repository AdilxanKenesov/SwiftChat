package uz.relay.app.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import uz.relay.core.navigation.AppNavigationHandler
import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.key.CallKey
import uz.relay.core.navigation.key.MediaViewerKey
import uz.relay.feature.auth.authEntries
import uz.relay.feature.calls.callsEntries
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
fun AppNavHost(navigationHandler: AppNavigationHandler, startKey: NavKey) {
    // Kalitlar @Serializable: stek jarayon o'ldirilganda (process death) ham saqlanib, qayta tiklanadi.
    val backStack = rememberNavBackStack(startKey)

    LaunchedEffect(navigationHandler) {
        navigationHandler.params.collect { param -> backStack.apply(param) }
    }

    // O'ngga surib orqaga: faqat ostida ekran bo'lsa va ekranning o'zi o'ngga surishni ishlatmasa.
    val top = backStack.lastOrNull()
    val swipeBackEnabled = backStack.size > 1 && top !is MediaViewerKey && top !is CallKey

    NavDisplay(
        backStack = backStack,
        modifier = Modifier.swipeBack(enabled = swipeBackEnabled),
        onBack = { backStack.pop() },
        // Telegram uslubi: yangi ekran o'ngdan kiradi, ostidagisi biroz chapga suriladi; orqaga — aksincha.
        transitionSpec = { slideIn(forward = true) },
        popTransitionSpec = { slideIn(forward = false) },
        // Gesture (tizim yoki bizning swipeBack) paytida ham xuddi shu "orqaga" harakati barmoq ortidan boradi.
        predictivePopTransitionSpec = { slideIn(forward = false) },
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
            callsEntries()
        }
    )
}

/**
 * Ekranlar orasidagi siljish. Oldinga: yangi ekran o'ngdan to'liq kiradi, eskisi 30% chapga suriladi. Orqaga:
 * yuqoridagi ekran o'ngga chiqib ketadi va **ustida** qoladi (`targetContentZIndex = -1`), ostidagisi -30% dan
 * joyiga keladi — barmoq bilan surganda oldingi ekran ostidan ochilib borayotgandek ko'rinadi.
 */
private fun slideIn(forward: Boolean): ContentTransform {
    val spec = tween<IntOffset>(durationMillis = TRANSITION_MS, easing = FastOutSlowInEasing)
    return if (forward) {
        slideInHorizontally(spec) { it } togetherWith slideOutHorizontally(spec) { -it / 3 }
    } else {
        (slideInHorizontally(spec) { -it / 3 } togetherWith slideOutHorizontally(spec) { it }).apply {
            targetContentZIndex = -1f
        }
    }
}

private const val TRANSITION_MS = 280

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
