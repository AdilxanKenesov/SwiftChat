package uz.relay.feature.chats.list.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.Avatar
import uz.relay.core.designsystem.component.ConnectionTitle
import uz.relay.core.designsystem.component.SwiftLogoTile
import uz.relay.core.designsystem.component.SwiftWordmark
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.ConnectionStatus
import uz.relay.domain.model.User
import uz.relay.feature.chats.R

/**
 * 64dp app bar: logo (34) + "SwiftChat" · qidiruv · mening avatarim (34).
 * Ulanish tugallanmaguncha logo va nom o'rniga holat ko'rsatiladi (spec 4-bo'lim):
 * sync ketayotganda "Yangilanmoqda…", socket ulanayotganda yoki internet yo'q bo'lsa "Ulanmoqda…".
 */
@Composable
internal fun ChatsTopBar(
    me: User?,
    connectionStatus: ConnectionStatus,
    onSearchClick: () -> Unit,
    onMyProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SwiftTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            val statusText = when (connectionStatus) {
                ConnectionStatus.UPDATING -> R.string.updating
                ConnectionStatus.CONNECTING, ConnectionStatus.OFFLINE -> R.string.connecting
                ConnectionStatus.CONNECTED -> null
            }
            if (statusText != null) {
                ConnectionTitle(text = stringResource(statusText), modifier = Modifier.padding(start = 4.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SwiftLogoTile(size = 34.dp, modifier = Modifier.padding(end = 12.dp))
                    SwiftWordmark(fontSize = 22.sp, letterSpacing = (-0.4).sp)
                }
            }
        }
        IconButton(onClick = onSearchClick) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_search),
                contentDescription = stringResource(R.string.search),
                tint = colors.text
            )
        }
        IconButton(onClick = onMyProfileClick) {
            // Profil hali yuklanmagan bo'lsa ham doira joyida turadi — app bar "sakramaydi".
            Avatar(
                name = me?.displayName,
                colorSeed = me?.id.orEmpty(),
                size = 34.dp,
                modifier = Modifier
            )
        }
    }
}
