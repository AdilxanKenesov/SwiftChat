package uz.relay.feature.conversation.chat.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.Message
import uz.relay.feature.conversation.R
import uz.relay.feature.conversation.chat.ChatItem
import uz.relay.feature.conversation.chat.canDelete
import uz.relay.feature.conversation.chat.canEdit
import uz.relay.feature.conversation.chat.canReply
import kotlin.math.max
import kotlin.math.min

/** Uzoq bosilgan xabar va uning bubble'i ekranning qayerida turgani (root koordinatalarda). */
data class MenuTarget(val item: ChatItem.Bubble, val bounds: Rect)

/**
 * Xabar menyusi (spec 3.8): qoraytirilgan fon + o'z joyida "ko'tarilgan" bubble + menyu (radius 18, 48dp qatorlar):
 * "Javob berish", "Tahrirlash" (o'zimniki, < 48 soat), "Nusxalash", ajratgich, "Oʻchirish" (qizil; o'zimniki
 * yoki guruhda OWNER/ADMIN bo'lsam — boshqaniki ham).
 *
 * Nega custom Layout: bubble ro'yxatdagi joyida chizilishi kerak, menyu esa uning ostida (joy bo'lmasa ustida)
 * va chiquvchi xabarda o'ng chetga, kiruvchida chap chetga tekislanadi. Joylar faqat o'lchangandan keyin
 * ma'lum — Layout ularni bitta o'lchash o'tishida hisoblaydi.
 */
@Composable
fun MessageMenuOverlay(
    target: MenuTarget,
    names: Map<String, String>,
    canDeleteOthers: Boolean,
    onReply: (Message) -> Unit,
    onEdit: (Message) -> Unit,
    onCopy: (Message) -> Unit,
    onDelete: (Message) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SwiftTheme.colors
    val message = target.item.message
    // Overlay'ning o'zi root'da qayerdan boshlanadi — bubble koordinatalarini shunga nisbatan olamiz.
    var origin by remember { mutableStateOf(Offset.Zero) }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .background(colors.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
    ) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                MessageBubble(
                    message = message,
                    senderName = if (target.item.showSenderName) names[message.senderId] else null,
                    replied = target.item.replied,
                    repliedSenderName = target.item.replied?.let { names[it.senderId] },
                    onReplyClick = {},
                    modifier = Modifier.shadow(elevation = 8.dp, shape = RoundedCornerShape(18.dp))
                )
                MenuCard(
                    message = message,
                    canDeleteOthers = canDeleteOthers,
                    onReply = { onReply(message) },
                    onEdit = { onEdit(message) },
                    onCopy = { onCopy(message) },
                    onDelete = { onDelete(message) }
                )
            }
        ) { measurables, constraints ->
            val bounds = target.bounds
            val bubble = measurables[0].measure(Constraints.fixedWidth(max(1, bounds.width.toInt())))
            val menu = measurables[1].measure(Constraints())
            val margin = 8.dp.roundToPx()
            val gap = 10.dp.roundToPx()
            val width = constraints.maxWidth
            val height = constraints.maxHeight

            layout(width, height) {
                val bubbleX = (bounds.left - origin.x).toInt()
                val bubbleY = (bounds.top - origin.y).toInt()
                bubble.place(bubbleX, bubbleY)

                val alignedX = if (message.isMine) bubbleX + bubble.width - menu.width else bubbleX
                val menuX = min(max(margin, alignedX), width - menu.width - margin)
                val below = bubbleY + bubble.height + gap
                val menuY = if (below + menu.height <= height - margin) below else max(margin, bubbleY - gap - menu.height)
                menu.place(menuX, menuY)
            }
        }
    }
}

@Composable
private fun MenuCard(
    message: Message,
    canDeleteOthers: Boolean,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = SwiftTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .width(216.dp)
            .shadow(elevation = 12.dp, shape = shape)
            .background(colors.menu, shape)
            .padding(vertical = 6.dp)
    ) {
        if (message.canReply()) MenuItem(DesignR.drawable.ic_reply, R.string.reply, colors.text, onReply)
        if (message.canEdit()) MenuItem(DesignR.drawable.ic_pencil, R.string.edit, colors.text, onEdit)
        if (!message.text.isNullOrEmpty()) MenuItem(DesignR.drawable.ic_copy, R.string.copy, colors.text, onCopy)
        if (message.canDelete(canDeleteOthers)) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = colors.line)
            MenuItem(DesignR.drawable.ic_trash, R.string.delete, colors.error, onDelete, bold = true)
        }
    }
}

@Composable
private fun MenuItem(icon: Int, label: Int, color: Color, onClick: () -> Unit, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(
            text = stringResource(label),
            color = color,
            fontSize = 15.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
