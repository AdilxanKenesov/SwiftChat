package uz.relay.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.relay.core.designsystem.theme.Brand

/** Brand background, white icon, colored glow. Same color in dark theme. */
@Composable
fun BrandTile(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    cornerRadius: Dp = 20.dp,
    iconSize: Dp = 26.dp,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 10.dp, shape = shape, ambientColor = Brand, spotColor = Brand)
            .background(Brand, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}
