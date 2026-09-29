package uz.relay.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.theme.FigtreeFontFamily
import uz.relay.core.designsystem.theme.SwiftTheme

/**
 * Dizayndagi 58dp chegarali (outlined) matn maydoni: yorlig'i (label) yuqori chegara ustida turadi.
 * Chegara: fokusda 2dp primary, xatoda 2dp error, aks holda 1.5dp outline.
 *
 * Nega Material OutlinedTextField emas: uning ichki padding'i, balandligi va label animatsiyasi dizaynga
 * mos kelmaydi. BasicTextField ustida qurilgani uchun har bir o'lcham to'liq nazoratda.
 */
@Composable
fun SwiftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    textStyle: TextStyle = TextStyle(fontSize = 17.sp),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = SwiftTheme.colors
    val focused by interactionSource.collectIsFocusedAsState()
    val accent = when {
        isError -> colors.error
        focused -> colors.primary
        else -> null
    }
    val shape = RoundedCornerShape(16.dp)

    // Maydon 9dp pastdan boshlanadi — shunda ~18dp balandlikdagi label qatori aynan yuqori chegara o'rtasida turadi.
    Box(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = textStyle.merge(TextStyle(color = colors.text, fontFamily = FigtreeFontFamily)),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            modifier = Modifier
                .padding(top = 9.dp)
                .fillMaxWidth()
                .height(58.dp)
                .border(if (accent != null) 2.dp else 1.5.dp, accent ?: colors.outline, shape),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leading?.invoke(this)
                    Box(modifier = Modifier.weight(1f)) { innerTextField() }
                    trailing?.invoke(this)
                }
            },
        )
        Text(
            text = label,
            color = accent ?: colors.text2,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(start = 12.dp)
                // Fon rangi label ortidagi chegara chizig'ini "kesib" turadi.
                .background(colors.bg)
                .padding(horizontal = 4.dp),
        )
    }
}
