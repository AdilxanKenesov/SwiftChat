package uz.relay.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import uz.relay.core.designsystem.theme.Brand
import uz.relay.core.designsystem.theme.SwiftTheme

/**
 * Dizayndagi FAB: 60×60, burchak radiusi 20, primary fon va Brand rangli soya.
 * Material'ning standart soyasi o'chiriladi — uning o'rniga rangli soya chiziladi.
 */
@Composable
fun SwiftFab(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SwiftTheme.colors
    val shape = RoundedCornerShape(20.dp)
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(60.dp)
            .shadow(elevation = 12.dp, shape = shape, ambientColor = Brand, spotColor = Brand),
        shape = shape,
        containerColor = colors.primary,
        contentColor = colors.onPrimary,
        // Material soyasi nolga tushiriladi — yuqoridagi `shadow` bilan rangli soya chiziladi.
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        Icon(painter = painterResource(icon), contentDescription = contentDescription, modifier = Modifier.size(24.dp))
    }
}
