package uz.relay.feature.conversation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.theme.FigtreeFontFamily
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.feature.conversation.R
import uz.relay.feature.conversation.chat.ChatContract

/**
 * Yozish paneli (spec 3.8): ixtiyoriy yuqori panel (javob yoki tahrir) · biriktirish · "Xabar" maydoni (pill, 44)
 * · yuborish tugmasi (44 doira).
 * Tugma holatlari: bo'sh → surface2/text2 va o'chiq; matn bor → primary + "yuborish"; tahrirda → "✓ saqlash".
 */
@Composable
fun Composer(
    text: String,
    mode: ChatContract.ComposerMode,
    modeSenderName: String?,
    modeSnippet: String?,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelMode: () -> Unit,
    onAttach: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SwiftTheme.colors
    val isEditing = mode is ChatContract.ComposerMode.Edit
    val canSend = text.isNotBlank()

    Column(modifier = modifier.background(colors.bg)) {
        if (mode !is ChatContract.ComposerMode.None) {
            ModePanel(
                isEditing = isEditing,
                title = if (isEditing) stringResource(R.string.edit) else modeSenderName.orEmpty(),
                snippet = modeSnippet.orEmpty(),
                onCancel = onCancelMode
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = onAttach, modifier = Modifier.size(44.dp)) {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_paperclip),
                    contentDescription = stringResource(R.string.attach),
                    tint = colors.text2,
                    modifier = Modifier.size(23.dp)
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                maxLines = 5,
                textStyle = TextStyle(color = colors.text, fontSize = 16.sp, fontFamily = FigtreeFontFamily),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .background(colors.surface, RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            Text(text = stringResource(R.string.message_hint), color = colors.text2, fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                }
            )
            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(44.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.surface2,
                    disabledContentColor = colors.text2
                ),
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(if (isEditing) DesignR.drawable.ic_check else DesignR.drawable.ic_send),
                    contentDescription = stringResource(if (isEditing) R.string.save else R.string.send),
                    modifier = Modifier.size(if (isEditing) 22.dp else 20.dp)
                )
            }
        }
    }
}

/** Javob / tahrir paneli: ikonka · chiziq · sarlavha (14/700 primary) + asl matn (14, bir qator) · ✕. */
@Composable
private fun ModePanel(isEditing: Boolean, title: String, snippet: String, onCancel: () -> Unit) {
    val colors = SwiftTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(if (isEditing) DesignR.drawable.ic_pencil else DesignR.drawable.ic_reply),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(22.dp)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(colors.primary, RoundedCornerShape(1.dp))
            )
            Column {
                Text(
                    text = title,
                    color = colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = snippet,
                    color = colors.text2,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(44.dp)) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_close),
                contentDescription = stringResource(R.string.cancel),
                tint = colors.text2,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
