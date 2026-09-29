package uz.relay.feature.chats.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.BrandTile
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.feature.chats.R

/** Chatlar yo'q holati: brend plitka (96, r32) + sarlavha + izoh + "Yangi chat" tugmasi. FAB yashiriladi. */
@Composable
internal fun EmptyChats(onNewChatClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SwiftTheme.colors
    Column(
        modifier = modifier.padding(start = 40.dp, end = 40.dp, bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandTile(icon = DesignR.drawable.ic_message_circle, size = 96.dp, cornerRadius = 32.dp, iconSize = 44.dp)
        Text(
            text = stringResource(R.string.no_chats),
            color = colors.text,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            text = stringResource(R.string.no_chats_sub),
            color = colors.text2,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
        Button(
            onClick = onNewChatClick,
            modifier = Modifier
                .padding(top = 24.dp)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(DesignR.drawable.ic_pencil), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = stringResource(R.string.new_chat), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
