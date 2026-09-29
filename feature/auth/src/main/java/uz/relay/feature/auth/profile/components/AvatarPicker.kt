package uz.relay.feature.auth.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.theme.Brand
import uz.relay.core.designsystem.theme.SwiftTheme

/**
 * 112dp avatar joy egallovchisi: kamerali primaryContainer doira, qo'sh halqa
 * (5dp fon oralig'i + 2dp primaryContainer) va 38dp brend rangli "+" belgisi.
 * Rasm yuklash media feature bilan qo'shiladi, hozircha faqat ko'rinish.
 *
 * Qatlamlar Box ichida ustma-ust qo'yilgan, offset o'rniga alignment/padding ishlatilgan.
 */
@Composable
fun AvatarPicker(contentDescription: String, modifier: Modifier = Modifier) {
    val colors = SwiftTheme.colors

    // 112 + 2 × (5dp oraliq + 2dp halqa) = 126
    Box(
        modifier = modifier
            .size(126.dp)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(126.dp)
                .border(2.dp, colors.primaryContainer, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(colors.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_camera),
                contentDescription = null,
                tint = colors.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }
        // Belgi ichki doiraning pastki o'ng burchagiga tekislanadi, shuning uchun 112dp o'lchamli qatlam ichida.
        Box(modifier = Modifier.size(112.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 2.dp)
                    .size(38.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape, ambientColor = Brand, spotColor = Brand)
                    // Fon rangidagi 3dp chegara belgini avatardan ajratib turadi.
                    .background(colors.bg, CircleShape)
                    .padding(3.dp)
                    .background(Brand, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_plus),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
