package uz.relay.feature.profile.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.Avatar
import uz.relay.core.designsystem.component.avatarColor
import uz.relay.core.designsystem.theme.Brand
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.feature.profile.R

/*
 * Profil ekranlari (o'zimniki va boshqa foydalanuvchiniki) uchun umumiy UI bo'laklari. Ular faqat shu feature'da
 * kerak, shuning uchun core:designsystem'ga chiqarilmagan — `internal` va dizayn spec'idagi o'lchamlar bilan.
 */

/**
 * Sozlama ikonkasi plitkalari ranglari (spec: "Settings icon tile"). Ikkala temada bir xil — oq ikonka
 * har qanday fonda ko'rinadi, rang esa qatorni ko'z bilan tez topishga yordam beradi.
 */
internal object TileColors {
    val Phone = Color(0xFF2F6BD6)
    val Username = Color(0xFF3D8A4F)
    val Notifications = Color(0xFFE0603A)
    val DarkMode = Color(0xFF6655E0)
    val Language = Color(0xFF0E8577)
}

/** 56dp tepa panel: orqaga · (bo'sh joy) · [actions]. Sarlavha yo'q — profil nomi pastda katta yoziladi. */
@Composable
internal fun ProfileTopBar(onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.back),
                tint = SwiftTheme.colors.text
            )
        }
        Box(modifier = Modifier.weight(1f))
        actions()
    }
}

/** Avatar 104 (rangli soya) · ism 24/700 · holat 14. */
@Composable
internal fun ProfileHeader(name: String?, colorSeed: String, status: String, statusColor: Color) {
    val colors = SwiftTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Avatar(
            name = name,
            colorSeed = colorSeed,
            size = 104.dp,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .shadow(elevation = 14.dp, shape = CircleShape, ambientColor = avatarColor(colorSeed), spotColor = avatarColor(colorSeed))
        )
        Text(
            text = name.orEmpty(),
            color = colors.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        if (status.isNotEmpty()) Text(text = status, color = statusColor, fontSize = 14.sp)
    }
}

/**
 * Sahifa kartasi (radius 20): yorug' temada yumshoq soya, tungi temada 1dp hoshiya — spec'dagi `page`
 * fonida kartani ajratib turuvchi usul temaga qarab farq qiladi.
 */
@Composable
internal fun ProfileCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = SwiftTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = if (colors.cardBorder == Color.Transparent) 2.dp else 0.dp, shape = shape)
            .clip(shape)
            .background(colors.card, shape)
            .border(1.dp, colors.cardBorder, shape)
            .padding(vertical = 4.dp),
        content = content
    )
}

/** 36×36 plitka (radius 11) ichida oq 19dp ikonka. */
@Composable
internal fun IconTile(@DrawableRes icon: Int, color: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color, RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
    }
}

/** Ma'lumot qatori: plitka · qiymat (16/500) + izoh (13, text2). Masalan: telefon, username. */
@Composable
internal fun InfoRow(@DrawableRes icon: Int, tileColor: Color, value: String, caption: String) {
    val colors = SwiftTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon = icon, color = tileColor)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = value, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = caption, color = colors.text2, fontSize = 13.sp)
        }
    }
}

/** 56dp sozlama qatori: plitka · nom · [trailing] (switch yoki qiymat). [onClick] bo'lsa butun qator bosiladi. */
@Composable
internal fun SettingRow(
    @DrawableRes icon: Int,
    tileColor: Color,
    label: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon = icon, color = tileColor)
        Text(text = label, color = SwiftTheme.colors.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Kartalar ustidagi kichik bo'lim sarlavhasi ("Hisob", "Sozlamalar") — kartalarni ma'no bo'yicha ajratadi. */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = SwiftTheme.colors.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = 32.dp, end = 32.dp, bottom = 8.dp)
    )
}

/**
 * Xavfli amal qatori (masalan, "Chiqish"): qizil ikonka plitkasi + qizil matn. Karta ichida, oddiy sozlama
 * qatorlari bilan bir xil o'lchamda — ekran pastiga yopishtirilgan alohida tugma o'rniga (u yoqmadi).
 * [loading] — amal ketmoqda: plitka o'rnida progress, qator bosilmaydi.
 */
@Composable
internal fun DangerRow(@DrawableRes icon: Int, label: String, onClick: () -> Unit, loading: Boolean = false) {
    val colors = SwiftTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(colors.errorContainer, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(color = colors.error, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(painter = painterResource(icon), contentDescription = null, tint = colors.error, modifier = Modifier.size(19.dp))
            }
        }
        Text(text = label, color = colors.error, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

/**
 * Foydalanuvchi profilidagi 136×64 amal kartasi (radius 20). [primary] — brand fon + oq matn va rangli soya
 * ("Xabar"), aks holda oddiy karta ("Ovozsiz qilish").
 */
@Composable
internal fun ProfileActionCard(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    enabled: Boolean = true,
    width: Dp = 136.dp
) {
    val colors = SwiftTheme.colors
    val shape = RoundedCornerShape(20.dp)
    // Dizaynda "Xabar" kartasi ikkala temada ham Brand rangida (logo plitkasi kabi), primary'da emas.
    val brand = Brand
    val shadowModifier = when {
        primary -> Modifier.shadow(elevation = 10.dp, shape = shape, ambientColor = brand, spotColor = brand)
        colors.cardBorder == Color.Transparent -> Modifier.shadow(elevation = 2.dp, shape = shape)
        else -> Modifier
    }
    val content = if (primary) Color.White else colors.text
    Column(
        modifier = Modifier
            .size(width = width, height = 64.dp)
            .then(shadowModifier)
            .clip(shape)
            .background(if (primary) brand else colors.card, shape)
            .border(1.dp, if (primary) Color.Transparent else colors.cardBorder, shape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        Text(text = label, color = content, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
