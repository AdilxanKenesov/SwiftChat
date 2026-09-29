package uz.relay.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import uz.relay.core.designsystem.theme.Brand
import uz.relay.core.designsystem.theme.SwiftTheme

/**
 * Ilova bo'yicha yagona tasdiqlash dialogi (M3 `AlertDialog` o'rniga).
 *
 * Nega o'zimizniki: standart AlertDialog chapga tekislangan sarlavha va kichik matnli tugmalar bilan "quruq"
 * ko'rinardi, har ekranda ranglar qo'lda berilardi. Bu yerda bir xil uslub: markazda rangli ikonka plitkasi,
 * qisqa savol, ikkita teng pill tugma (chapda "Bekor qilish", o'ngda asosiy amal). Xavfli amalda ([destructive]:
 * chiqish, o'chirish) plitka va tugma qizil — foydalanuvchi oqibatini ko'z bilan ham sezadi.
 *
 * @param text ixtiyoriy qisqa izoh — savolning o'zi yetarli bo'lsa berilmaydi.
 * @param content ixtiyoriy qo'shimcha kontent (masalan, [SwiftInputDialog]dagi matn maydoni).
 */
@Composable
fun SwiftDialog(
    title: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    @DrawableRes icon: Int? = null,
    text: String? = null,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colors = SwiftTheme.colors
    val accent = if (destructive) colors.error else colors.primary
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(colors.bg)
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (destructive) colors.errorContainer else colors.primaryContainer, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                }
            }
            Text(
                text = title,
                color = colors.text,
                fontSize = 19.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = if (icon != null) 16.dp else 0.dp)
            )
            if (text != null) {
                Text(
                    text = text,
                    color = colors.text2,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (content != null) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), content = content)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DialogButton(
                    text = dismissText,
                    container = colors.surface2,
                    content = colors.text,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                DialogButton(
                    text = confirmText,
                    container = accent,
                    content = if (destructive) Color.White else colors.onPrimary,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Bitta matn maydonli dialog (masalan, guruh nomini o'zgartirish). Maydon ochilishi bilan fokus oladi va
 * klaviatura chiqadi; asosiy tugma matn yaroqli ([isValid]) va boshlang'ichdan farqli bo'lgandagina faol.
 * Matn holati shu yerda (UI'ga tegishli, ViewModel faqat natijani oladi).
 */
@Composable
fun SwiftInputDialog(
    title: String,
    label: String,
    initialValue: String,
    confirmText: String,
    dismissText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    @DrawableRes icon: Int? = null,
    maxLength: Int = Int.MAX_VALUE,
    isValid: (String) -> Boolean = { it.isNotBlank() }
) {
    // Kursor matn oxirida turadi — nomni to'ldirish yoki tuzatish uchun qulay.
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialValue, TextRange(initialValue.length)))
    }
    val trimmed = value.text.trim()
    val canConfirm = isValid(trimmed) && trimmed != initialValue.trim()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    SwiftDialog(
        title = title,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = { if (canConfirm) onConfirm(trimmed) },
        onDismiss = onDismiss,
        icon = icon,
        confirmEnabled = canConfirm
    ) {
        SwiftTextField(
            value = value.text,
            onValueChange = { value = TextFieldValue(it.take(maxLength), TextRange(it.take(maxLength).length)) },
            label = label,
            modifier = Modifier.focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (canConfirm) onConfirm(trimmed) })
        )
    }
}

/** 48dp pill tugma. O'chiq holat — shaffofroq (bosib bo'lmasligi ko'rinib tursin). */
@Composable
private fun DialogButton(
    text: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = content, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/**
 * Ilova switch'i: yoqilganda Brand trek + oq tugma (ikkala temada bir xil — brend rangi), o'chirilganda
 * yumshoq kulrang trek va oq tugma, hoshiyasiz; tugma ikkala holatda bir xil kattalikda. Standart M3 ranglari (primary/outline) tungi temada xira va "begona"
 * ko'rinardi — shuning uchun ranglar shu yerda bir marta belgilanadi.
 */
@Composable
fun SwiftSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val colors = SwiftTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        // Bo'sh thumbContent — M3'da tugma o'chiq holatda kichrayib ketmaydi, ikkala holatda bir xil (24dp).
        thumbContent = { Box(modifier = Modifier.size(SwitchDefaults.IconSize)) },
        colors = SwitchDefaults.colors(
            checkedTrackColor = Brand,
            checkedThumbColor = Color.White,
            checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = colors.muted,
            uncheckedThumbColor = Color.White,
            uncheckedBorderColor = Color.Transparent
        )
    )
}
