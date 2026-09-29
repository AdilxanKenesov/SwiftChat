package uz.relay.feature.group.info

import uz.relay.core.designsystem.component.SwiftSnackbarHost
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.Avatar
import uz.relay.core.designsystem.component.BrandTile
import uz.relay.core.designsystem.component.SwiftTextField
import uz.relay.core.designsystem.component.avatarColor
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.core.designsystem.util.formatPresence
import uz.relay.domain.model.ChatMember
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.GroupPermissions
import uz.relay.domain.model.MemberRole
import uz.relay.feature.group.R
import uz.relay.feature.group.util.messageRes

/**
 * Guruh ma'lumotlari ekranining kirish nuqtasi (stateful qism).
 *
 * Chat ekranidagi guruh sarlavhasi bosilganda ochiladi. Bu yerdan: chat ichida qidiruv, a'zo qo'shish
 * (guruh yaratish ekrani qayta ishlatiladi), a'zo bilan shaxsiy chat va guruhdan chiqish (chatlar ro'yxatiga).
 *
 * Funksiya ViewModel'ni oladi, holatni yig'adi va SideEffect'larni (snackbar) ushlaydi; chizish stateless
 * [GroupInfoContent] da — Preview va testlarda ViewModel'siz ishlatish uchun.
 */
@Composable
internal fun GroupInfoScreen(chatId: String) {
    // AssistedInject: runtime argument `chatId` factory orqali ViewModel'ga uzatiladi.
    val viewModel = hiltViewModel<GroupInfoViewModel, GroupInfoViewModel.Factory>(
        creationCallback = { factory -> factory.create(chatId) }
    )
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is GroupInfoContract.SideEffect.ShowError ->
                snackbarHostState.showSnackbar(context.getString(sideEffect.error.messageRes()))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GroupInfoContent(uiState = uiState, onEventDispatcher = viewModel::onEventDispatcher)
        SwiftSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * Ekranning stateless qismi: yuqori panel · sarlavha · amal kartalari · a'zolar ro'yxati · "Guruhdan chiqish".
 * Butun kontent bitta [LazyColumn] da — a'zolar ko'p bo'lsa ham faqat ko'rinadiganlari chiziladi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupInfoContent(
    uiState: GroupInfoContract.UiState,
    onEventDispatcher: (GroupInfoContract.Intent) -> Unit
) {
    val colors = SwiftTheme.colors
    val resources = LocalContext.current.resources
    val chat = uiState.chat

    // Dialog va sheet holatlari — faqat ko'rinishga tegishli, shuning uchun UI'da saqlanadi.
    var selectedMember by remember { mutableStateOf<ChatMember?>(null) }
    var confirmRemove by remember { mutableStateOf<ChatMember?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.page)
            .statusBarsPadding()
    ) {
        // 56dp: orqaga · (bo'sh joy) · tahrirlash (faqat ADMIN/OWNER).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onEventDispatcher(GroupInfoContract.Intent.OnBack) }) {
                Icon(painter = painterResource(DesignR.drawable.ic_arrow_left), contentDescription = stringResource(R.string.back), tint = colors.text)
            }
            Box(modifier = Modifier.weight(1f))
            if (uiState.canManage) {
                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(
                        painter = painterResource(DesignR.drawable.ic_pencil),
                        contentDescription = stringResource(R.string.edit),
                        tint = colors.text,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
            item(key = "header") { Header(chat = chat, memberCount = uiState.members.size, onlineCount = uiState.onlineCount) }
            item(key = "actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    ActionCard(
                        icon = if (chat?.muted == true) DesignR.drawable.ic_bell_off else DesignR.drawable.ic_bell,
                        label = stringResource(R.string.mute),
                        active = chat?.muted == true,
                        onClick = { onEventDispatcher(GroupInfoContract.Intent.OnToggleMute) }
                    )
                    ActionCard(
                        icon = DesignR.drawable.ic_search,
                        label = stringResource(R.string.search),
                        onClick = { onEventDispatcher(GroupInfoContract.Intent.OnSearch) }
                    )
                    if (uiState.canManage) {
                        ActionCard(
                            icon = DesignR.drawable.ic_user_plus,
                            label = stringResource(R.string.add),
                            onClick = { onEventDispatcher(GroupInfoContract.Intent.OnAddMembers) }
                        )
                    }
                }
            }
            item(key = "members-label") {
                Text(
                    text = stringResource(R.string.members),
                    color = colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)
                )
            }
            if (uiState.canManage) {
                item(key = "add-member") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { onEventDispatcher(GroupInfoContract.Intent.OnAddMembers) }
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrandTile(icon = DesignR.drawable.ic_user_plus, size = 40.dp, cornerRadius = 14.dp, iconSize = 20.dp)
                        Text(text = stringResource(R.string.add_members), color = colors.primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            items(items = uiState.members, key = { it.userId }) { member ->
                MemberRow(
                    member = member,
                    status = formatPresence(member.online, member.lastSeenAt, resources),
                    highlighted = member.userId == selectedMember?.userId,
                    // O'zimni bosishning ma'nosi yo'q; boshqa a'zo — amallar sheet'i (ruxsatga qarab).
                    onClick = if (member.isMe) null else ({ selectedMember = member })
                )
            }
        }

        // Pastki qator: "Guruhdan chiqish" (qizil) + tepada chiziq.
        HorizontalDivider(thickness = 1.dp, color = colors.line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp)
                .clickable { showLeaveDialog = true }
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painter = painterResource(DesignR.drawable.ic_log_out), contentDescription = null, tint = colors.error, modifier = Modifier.size(22.dp))
            Text(text = stringResource(R.string.leave_group), color = colors.error, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    // Sheet va dialoglar Column'dan tashqarida — ular o'z oynasida (overlay) ko'rsatiladi.
    selectedMember?.let { member ->
        MemberSheet(
            member = member,
            myRole = uiState.myRole,
            onDismiss = { selectedMember = null },
            onChangeRole = { role ->
                selectedMember = null
                onEventDispatcher(GroupInfoContract.Intent.OnChangeRole(member, role))
            },
            onWriteMessage = {
                selectedMember = null
                onEventDispatcher(GroupInfoContract.Intent.OnWriteMessage(member))
            },
            onRemove = {
                selectedMember = null
                confirmRemove = member
            }
        )
    }

    confirmRemove?.let { member ->
        ConfirmDialog(
            title = stringResource(R.string.remove_title, member.displayName ?: stringResource(R.string.unknown_user)),
            text = null,
            confirm = stringResource(R.string.remove_confirm),
            onConfirm = {
                confirmRemove = null
                onEventDispatcher(GroupInfoContract.Intent.OnRemove(member))
            },
            onDismiss = { confirmRemove = null }
        )
    }

    if (showLeaveDialog) {
        ConfirmDialog(
            title = stringResource(R.string.leave_title),
            text = stringResource(R.string.leave_text),
            confirm = stringResource(R.string.leave_confirm),
            onConfirm = {
                showLeaveDialog = false
                onEventDispatcher(GroupInfoContract.Intent.OnLeave)
            },
            onDismiss = { showLeaveDialog = false }
        )
    }

    if (showRenameDialog) {
        RenameDialog(
            initial = chat?.title.orEmpty(),
            onSave = { title ->
                showRenameDialog = false
                onEventDispatcher(GroupInfoContract.Intent.OnRename(title))
            },
            onDismiss = { showRenameDialog = false }
        )
    }
}

/** Avatar 88 (rangli soya bilan) · nom (23/700) · "N aʼzo · M online". */
@Composable
private fun Header(chat: ChatSummary?, memberCount: Int, onlineCount: Int) {
    val colors = SwiftTheme.colors
    val seed = chat?.id.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Avatar(
            name = chat?.title,
            colorSeed = seed,
            size = 88.dp,
            modifier = Modifier
                .padding(bottom = 10.dp)
                .shadow(elevation = 14.dp, shape = CircleShape, ambientColor = avatarColor(seed), spotColor = avatarColor(seed))
        )
        Text(
            text = chat?.title.orEmpty(),
            color = colors.text,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        if (memberCount > 0) {
            Text(text = stringResource(R.string.members_online, pluralStringResource(R.plurals.members_n, memberCount, memberCount), onlineCount), color = colors.text2, fontSize = 14.sp)
        }
    }
}

/** 98×72 karta (radius 20): primary ikonka + yorliq (13/700). Faol holatda (ovozsiz) primaryContainer fon. */
@Composable
private fun ActionCard(icon: Int, label: String, onClick: () -> Unit, active: Boolean = false) {
    val colors = SwiftTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .width(98.dp)
            .height(72.dp)
            // Soya faqat yorug' temada (spec: dark'da karta hoshiya bilan ajraladi).
            .shadow(elevation = if (colors.cardBorder == Color.Transparent) 2.dp else 0.dp, shape = shape)
            .clip(shape)
            .background(if (active) colors.primaryContainer else colors.card, shape)
            .border(1.dp, colors.cardBorder, shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
        Text(text = label, color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** 60dp: avatar 40 · ism (+ " (siz)") + holat · rol belgisi (Egasi / Admin). */
@Composable
private fun MemberRow(member: ChatMember, status: String, highlighted: Boolean, onClick: (() -> Unit)?) {
    val colors = SwiftTheme.colors
    val name = member.displayName ?: stringResource(R.string.unknown_user)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(if (highlighted) colors.surface2 else Color.Transparent)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = member.displayName, colorSeed = member.userId, size = 40.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = if (member.isMe) stringResource(R.string.you_suffix, name) else name,
                color = colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = status, color = if (member.online) colors.primary else colors.text2, fontSize = 13.sp, maxLines = 1)
        }
        RolePill(role = member.role)
    }
}

/** OWNER — "Egasi" (primaryContainer), ADMIN — "Admin" (surface2), oddiy a'zoda belgi yo'q. */
@Composable
private fun RolePill(role: MemberRole) {
    val colors = SwiftTheme.colors
    val (label, container, content) = when (role) {
        MemberRole.OWNER -> Triple(R.string.owner, colors.primaryContainer, colors.onPrimaryContainer)
        MemberRole.ADMIN -> Triple(R.string.admin, colors.surface2, colors.text2)
        else -> return
    }
    Text(
        text = stringResource(label),
        color = content,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(container, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

/**
 * A'zo amallari (spec 3.11): sarlavha (avatar + ism + rol) · "Admin qilish"/"Aʼzo qilish" (faqat OWNER)
 * · "Xabar yozish" · "Guruhdan chiqarish" (OWNER har kimni, ADMIN faqat oddiy a'zoni). Ruxsat yo'q amal ko'rinmaydi.
 *
 * Nega ModalBottomSheet: amallar ro'yxati kontekstli (bitta a'zoga tegishli), ekrandan chiqmasdan pastdan ochiladi
 * va tashqariga bosish/pastga surish bilan yopiladi — alohida ekran yoki menyu kerak emas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberSheet(
    member: ChatMember,
    myRole: MemberRole?,
    onDismiss: () -> Unit,
    onChangeRole: (MemberRole) -> Unit,
    onWriteMessage: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = SwiftTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.bg,
        scrimColor = colors.scrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline, width = 32.dp, height = 4.dp) }
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(name = member.displayName, colorSeed = member.userId, size = 48.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = member.displayName ?: stringResource(R.string.unknown_user),
                        color = colors.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = stringResource(roleLabel(member.role)), color = colors.text2, fontSize = 14.sp)
                }
            }
            if (GroupPermissions.canChangeRole(myRole, member)) {
                val makeAdmin = member.role != MemberRole.ADMIN
                SheetItem(
                    icon = DesignR.drawable.ic_shield_check,
                    label = stringResource(if (makeAdmin) R.string.make_admin else R.string.make_member),
                    color = colors.text,
                    onClick = { onChangeRole(if (makeAdmin) MemberRole.ADMIN else MemberRole.MEMBER) }
                )
            }
            SheetItem(
                icon = DesignR.drawable.ic_message_circle,
                label = stringResource(R.string.write_message),
                color = colors.text,
                onClick = onWriteMessage
            )
            if (GroupPermissions.canRemove(myRole, member)) {
                SheetItem(
                    icon = DesignR.drawable.ic_user_minus,
                    label = stringResource(R.string.remove_from_group),
                    color = colors.error,
                    bold = true,
                    onClick = onRemove
                )
            }
        }
    }
}

/** Rol → matn resursi (sheet sarlavhasidagi rol nomi uchun). */
private fun roleLabel(role: MemberRole): Int = when (role) {
    MemberRole.OWNER -> R.string.owner
    MemberRole.ADMIN -> R.string.admin
    else -> R.string.member
}

/** Sheet ichidagi bitta amal qatori (56dp): ikonka + yorliq, rang amal turiga qarab (xavfli — qizil). */
@Composable
private fun SheetItem(icon: Int, label: String, color: Color, onClick: () -> Unit, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(text = label, color = color, fontSize = 16.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium)
    }
}

/** Xavfli amallar (a'zoni chiqarish, guruhdan chiqish) uchun tasdiqlash dialogi — tasodifiy bosishdan himoya. */
@Composable
private fun ConfirmDialog(title: String, text: String?, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = SwiftTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.menu,
        title = { Text(title, color = colors.text) },
        text = text?.let { { Text(it, color = colors.text2) } },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = colors.error, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.primary) }
        }
    )
}

/** Guruh nomini o'zgartirish (ADMIN/OWNER). Server qoidasi: 1..128 belgi. */
@Composable
private fun RenameDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = SwiftTheme.colors
    // Kiritilayotgan nom vaqtinchalik UI holati — faqat "Saqlash" bosilganda ViewModel'ga yuboriladi.
    var title by remember { mutableStateOf(initial) }
    val valid = title.trim().length in 1..128 && title.trim() != initial
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.menu,
        title = { Text(stringResource(R.string.rename_title), color = colors.text) },
        text = {
            SwiftTextField(
                value = title,
                onValueChange = { title = it.take(128) },
                label = stringResource(R.string.group_name)
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(title.trim()) }, enabled = valid) {
                Text(stringResource(R.string.save), color = if (valid) colors.primary else colors.text2, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.primary) }
        }
    )
}

// ---------------- Preview'lar ----------------
// Stateless GroupInfoContent: OWNER (boshqaruv tugmalari bilan) va oddiy MEMBER ko'rinishi, yorug'/qorong'i temada.

private val PreviewGroup = ChatSummary(
    id = "g1",
    type = ChatType.GROUP,
    title = "Loyiha jamoasi",
    peerUserId = null,
    peerOnline = false,
    peerLastSeenAt = null,
    lastMessage = null,
    lastActivityAt = 0,
    unreadCount = 0,
    muted = false
)

private fun previewMembers(meRole: MemberRole) = listOf(
    ChatMember("me", "Dawran", meRole, online = true, lastSeenAt = null, isMe = true),
    ChatMember("m1", "Malika Yusupova", if (meRole == MemberRole.OWNER) MemberRole.ADMIN else MemberRole.OWNER, true, null, false),
    ChatMember("m2", "Jasur Aliyev", MemberRole.MEMBER, false, System.currentTimeMillis() - 300_000, false),
    ChatMember("m3", "Ali Valiyev", MemberRole.MEMBER, true, null, false),
    ChatMember("m4", "Sardor Rahimov", MemberRole.MEMBER, false, System.currentTimeMillis() - 3_600_000, false)
)

@Composable
private fun GroupInfoPreview(darkTheme: Boolean, meRole: MemberRole) {
    SwiftChatTheme(darkTheme = darkTheme) {
        GroupInfoContent(
            uiState = GroupInfoContract.UiState(chat = PreviewGroup, members = previewMembers(meRole)),
            onEventDispatcher = {}
        )
    }
}

@Preview(name = "Owner · Light", showSystemUi = true)
@Composable
private fun GroupInfoOwnerLightPreview() = GroupInfoPreview(false, MemberRole.OWNER)

@Preview(name = "Owner · Dark", showSystemUi = true)
@Composable
private fun GroupInfoOwnerDarkPreview() = GroupInfoPreview(true, MemberRole.OWNER)

@Preview(name = "Member · Light", showSystemUi = true)
@Composable
private fun GroupInfoMemberLightPreview() = GroupInfoPreview(false, MemberRole.MEMBER)

@Preview(name = "Member · Dark", showSystemUi = true)
@Composable
private fun GroupInfoMemberDarkPreview() = GroupInfoPreview(true, MemberRole.MEMBER)
