package uz.relay.feature.group.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.Avatar
import uz.relay.core.designsystem.component.SwiftFab
import uz.relay.core.designsystem.component.SwiftSnackbarHost
import uz.relay.core.designsystem.component.SwiftTextField
import uz.relay.core.designsystem.theme.Brand
import uz.relay.core.designsystem.theme.FigtreeFontFamily
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.core.designsystem.util.formatPresence
import uz.relay.domain.model.User
import uz.relay.feature.group.R
import uz.relay.feature.group.util.messageRes

/**
 * "Yangi guruh" / "A'zo qo'shish" ekranining kirish nuqtasi (stateful qism).
 *
 * Ochiladi: chatlar qidiruvidagi "Yangi guruh" dan (`addToChatId = null`) yoki guruh ma'lumotlari ekranidagi
 * "Qo'shish" dan (`addToChatId = chatId`). Yaratilgach — yangi guruh chatiga, qo'shilgach — orqaga qaytadi.
 *
 * Bu funksiya faqat ViewModel'ni oladi, holatni yig'adi va SideEffect'larni (snackbar) ushlaydi; chizishni
 * stateless [GroupCreateContent] bajaradi — shuning uchun uni Preview'da ViewModel'siz ko'rsatish mumkin.
 */
@Composable
internal fun GroupCreateScreen(addToChatId: String?) {
    // AssistedInject: runtime argument `addToChatId` factory orqali ViewModel'ga uzatiladi.
    val viewModel = hiltViewModel<GroupCreateViewModel, GroupCreateViewModel.Factory>(
        creationCallback = { factory -> factory.create(addToChatId) }
    )
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // Nom qadamida "orqaga" (tizim tugmasi, gesture yoki o'ngga surish) ekranni yopmasin — tanlash qadamiga qaytaradi.
    BackHandler(enabled = uiState.step == GroupCreateContract.Step.NAME) {
        viewModel.onEventDispatcher(GroupCreateContract.Intent.OnBack)
    }
    val resources = LocalResources.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is GroupCreateContract.SideEffect.ShowError ->
                snackbarHostState.showSnackbar(resources.getString(sideEffect.error.messageRes()))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GroupCreateContent(uiState = uiState, onEventDispatcher = viewModel::onEventDispatcher)
        SwiftSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * Ekranning stateless qismi: faqat `uiState` ni chizadi va harakatlarni `onEventDispatcher` ga yuboradi.
 * Qadamga qarab [PickStep] yoki [NameStep] ko'rsatiladi; FAB esa ular ustida pastki o'ng burchakda turadi.
 */
@Composable
internal fun GroupCreateContent(
    uiState: GroupCreateContract.UiState,
    onEventDispatcher: (GroupCreateContract.Intent) -> Unit
) {
    val colors = SwiftTheme.colors
    val isPick = uiState.step == GroupCreateContract.Step.PICK

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title = stringResource(if (uiState.isAddMode) R.string.add_members else R.string.new_group),
                subtitle = if (isPick) pluralStringResource(R.plurals.selected_n, uiState.selected.size, uiState.selected.size) else stringResource(R.string.name_and_photo),
                onBack = { onEventDispatcher(GroupCreateContract.Intent.OnBack) }
            )
            if (isPick) PickStep(uiState, onEventDispatcher) else NameStep(uiState, onEventDispatcher)
        }

        // FAB: 1-qadamda "keyingi" (kamida bitta tanlanganda), 2-qadamda "✓ yaratish". Yuborilayotganda — spinner.
        val showFab = if (isPick) uiState.canProceed || uiState.isSubmitting else uiState.canCreate || uiState.isSubmitting
        if (showFab) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 20.dp)
            ) {
                if (uiState.isSubmitting) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(colors.primary, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.onPrimary, strokeWidth = 2.5.dp)
                    }
                } else if (isPick) {
                    SwiftFab(
                        icon = if (uiState.isAddMode) DesignR.drawable.ic_check else DesignR.drawable.ic_arrow_right,
                        contentDescription = stringResource(if (uiState.isAddMode) R.string.add_members else R.string.next),
                        onClick = { onEventDispatcher(GroupCreateContract.Intent.OnNext) }
                    )
                } else {
                    SwiftFab(
                        icon = DesignR.drawable.ic_check,
                        contentDescription = stringResource(R.string.create_group),
                        onClick = { onEventDispatcher(GroupCreateContract.Intent.OnCreate) }
                    )
                }
            }
        }
    }
}

/** 64dp yuqori panel: orqaga tugmasi · sarlavha + izoh (tanlanganlar soni yoki "Nom va rasm"). */
@Composable
private fun TopBar(title: String, subtitle: String, onBack: () -> Unit) {
    val colors = SwiftTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(painter = painterResource(DesignR.drawable.ic_arrow_left), contentDescription = stringResource(R.string.back), tint = colors.text)
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = title, color = colors.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = colors.text2, fontSize = 13.sp)
        }
    }
}

/** 1-qadam: tanlanganlar chip'lari · pill qidiruv maydoni · odamlar ro'yxati (dumaloq checkbox bilan). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickStep(uiState: GroupCreateContract.UiState, onEventDispatcher: (GroupCreateContract.Intent) -> Unit) {
    val colors = SwiftTheme.colors
    val resources = LocalResources.current

    // FlowRow: chip'lar qatorga sig'masa keyingi qatorga o'tadi — gorizontal scroll'siz hammasi ko'rinadi.
    if (uiState.selected.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.selected.forEach { user ->
                SelectedChip(user = user, onRemove = { onEventDispatcher(GroupCreateContract.Intent.OnToggle(user)) })
            }
        }
    }

    PillSearchField(
        query = uiState.query,
        onQueryChange = { onEventDispatcher(GroupCreateContract.Intent.OnQueryChange(it)) }
    )

    if (uiState.candidates.isEmpty()) {
        // Bo'sh holat — bitta qisqa qator, markazda (ortiqcha ko'rsatma matnisiz).
        Text(
            text = stringResource(R.string.no_people),
            color = colors.text2,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Oxirgi qator FAB ostida qolmasin.
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        items(items = uiState.candidates, key = { it.id }) { user ->
            val checked = user.id in uiState.selectedIds
            PersonRow(
                user = user,
                status = formatPresence(user.online, user.lastSeenAt, resources),
                modifier = Modifier.clickable { onEventDispatcher(GroupCreateContract.Intent.OnToggle(user)) }
            ) {
                RoundCheckbox(checked = checked)
            }
        }
    }
}

/** 2-qadam: guruh avatari o'rni (72, Brand) + "Guruh nomi" maydoni · "N aʼzo" · tanlanganlar ro'yxati. */
@Composable
private fun NameStep(uiState: GroupCreateContract.UiState, onEventDispatcher: (GroupCreateContract.Intent) -> Unit) {
    val colors = SwiftTheme.colors
    val resources = LocalResources.current

    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Guruh rasmi qo'yilmaydi (server rasmni boshqa a'zolarga bermaydi) — nomning bosh harfi ko'rsatiladi.
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(elevation = 10.dp, shape = CircleShape, ambientColor = Brand, spotColor = Brand)
                .background(Brand, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(DesignR.drawable.ic_users), contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        SwiftTextField(
            value = uiState.title,
            onValueChange = { onEventDispatcher(GroupCreateContract.Intent.OnTitleChange(it)) },
            label = stringResource(R.string.group_name),
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done)
        )
    }
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(colors.surface)
    )
    Text(
        text = pluralStringResource(R.plurals.members_n, uiState.selected.size, uiState.selected.size),
        color = colors.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
    )
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        items(items = uiState.selected, key = { it.id }) { user ->
            PersonRow(user = user, status = formatPresence(user.online, user.lastSeenAt, resources))
        }
    }
}

/** 64dp: avatar 46 · ism (16/600) + holat (13; online — primary) · o'ng tomonda ixtiyoriy element. */
@Composable
private fun PersonRow(
    user: User,
    status: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    val colors = SwiftTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 16.dp, end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = user.displayName, colorSeed = user.id, size = 46.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = user.displayName,
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = status, color = if (user.online) colors.primary else colors.text2, fontSize = 13.sp, maxLines = 1)
        }
        trailing()
    }
}

/** Belgilangan: primary doira ichida oq ✓; belgilanmagan: 2dp hoshiyali bo'sh doira. */
@Composable
private fun RoundCheckbox(checked: Boolean) {
    val colors = SwiftTheme.colors
    if (checked) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(colors.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(DesignR.drawable.ic_check), contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(15.dp))
        }
    } else {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(2.dp, colors.outline, CircleShape)
        )
    }
}

/** Tanlangan odam chip'i: avatar 28 · ism (birinchi so'z) · ✕. Bosilsa tanlovdan chiqadi. */
@Composable
private fun SelectedChip(user: User, onRemove: () -> Unit) {
    val colors = SwiftTheme.colors
    val shape = RoundedCornerShape(17.dp)
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(shape)
            .background(colors.primaryContainer, shape)
            .clickable(onClick = onRemove)
            .padding(start = 3.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = user.displayName, colorSeed = user.id, size = 28.dp)
        Text(
            text = user.displayName.substringBefore(' '),
            color = colors.onPrimaryContainer,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            painter = painterResource(DesignR.drawable.ic_close),
            contentDescription = stringResource(R.string.remove_selected),
            tint = colors.onPrimaryContainer,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Pill ko'rinishidagi qidiruv maydoni. Material TextField o'rniga [BasicTextField] + `decorationBox`:
 * dizayndagi dumaloq fon, ikonka va placeholder'ni aniq chizish uchun (Material'ning ichki padding/label'isiz).
 */
@Composable
private fun PillSearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors = SwiftTheme.colors
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
            .height(48.dp)
            .background(colors.surface, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(DesignR.drawable.ic_search), contentDescription = null, tint = colors.text2, modifier = Modifier.size(20.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = colors.text, fontSize = 16.sp, fontFamily = FigtreeFontFamily),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false, imeAction = ImeAction.Search),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text(stringResource(R.string.search_hint), color = colors.text2, fontSize = 16.sp)
                    inner()
                }
            }
        )
    }
}

// ---------------- Preview'lar ----------------
// Stateless GroupCreateContent tayyor holat bilan chiziladi: ikkala qadam yorug' va qorong'i temada.

private val PreviewPeople = listOf(
    User("1", "jasur", "Jasur Aliyev", null, 0, null, online = true),
    User("2", "malika", "Malika Yusupova", null, 0, null, lastSeenAt = System.currentTimeMillis() - 3_600_000),
    User("3", "sardor", "Sardor Rahimov", null, 0, null, online = true),
    User("4", "dilnoza", "Dilnoza Ergasheva", null, 0, null)
)

@Composable
private fun GroupCreatePreview(darkTheme: Boolean, state: GroupCreateContract.UiState) {
    SwiftChatTheme(darkTheme = darkTheme) { GroupCreateContent(uiState = state, onEventDispatcher = {}) }
}

@Preview(name = "Pick · Light", showSystemUi = true)
@Composable
private fun PickLightPreview() =
    GroupCreatePreview(false, GroupCreateContract.UiState(candidates = PreviewPeople, selected = PreviewPeople.take(3)))

@Preview(name = "Pick · Dark", showSystemUi = true)
@Composable
private fun PickDarkPreview() =
    GroupCreatePreview(true, GroupCreateContract.UiState(candidates = PreviewPeople, selected = PreviewPeople.take(3)))

@Preview(name = "Name · Light", showSystemUi = true)
@Composable
private fun NameLightPreview() = GroupCreatePreview(
    false,
    GroupCreateContract.UiState(step = GroupCreateContract.Step.NAME, selected = PreviewPeople.take(3), title = "Dizayn jamoasi")
)

@Preview(name = "Name · Dark", showSystemUi = true)
@Composable
private fun NameDarkPreview() = GroupCreatePreview(
    true,
    GroupCreateContract.UiState(step = GroupCreateContract.Step.NAME, selected = PreviewPeople.take(3), title = "Dizayn jamoasi")
)
