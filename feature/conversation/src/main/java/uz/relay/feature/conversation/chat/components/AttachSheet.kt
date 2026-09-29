package uz.relay.feature.conversation.chat.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.Attachment
import uz.relay.feature.conversation.R
import java.io.File

/**
 * Biriktirish: Galereya · Kamera · Fayl (spec "Attach sheet").
 *
 *  - Galereya — tizim Photo Picker'i: hech qanday ruxsat so'ralmaydi, faqat tanlangan fayl beriladi.
 *  - Kamera — tizim kamera ilovasi suratni biz bergan FileProvider fayliga yozadi (CAMERA ruxsati kerak emas,
 *    chunki suratni kamera ilovasining o'zi oladi).
 *  - Fayl — istalgan hujjat; rasm tanlansa ham siqilmagan FILE sifatida ketadi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttachSheet(
    onDismiss: () -> Unit,
    onPicked: (Attachment) -> Unit,
    onCameraUnavailable: () -> Unit
) {
    val colors = SwiftTheme.colors
    val context = LocalContext.current
    // Kamera fayli yo'li burilish/process o'limidan keyin ham kerak — natija qaytganda o'qiladi.
    var cameraPath by rememberSaveable { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(Attachment(uri.toString(), asFile = false)) else onDismiss()
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPicked(Attachment(uri.toString(), asFile = true)) else onDismiss()
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = cameraPath
        if (saved && path != null) onPicked(Attachment(Uri.fromFile(File(path)).toString(), asFile = false)) else onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.bg,
        scrimColor = colors.scrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline, width = 32.dp, height = 4.dp) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttachOption(icon = DesignR.drawable.ic_image, label = stringResource(R.string.gallery), tint = Color(0xFF2F6BD6)) {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            }
            AttachOption(icon = DesignR.drawable.ic_camera, label = stringResource(R.string.camera), tint = Color(0xFFE0603A)) {
                val file = newCameraFile(context)
                cameraPath = file.path
                try {
                    cameraLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                } catch (e: ActivityNotFoundException) {
                    onCameraUnavailable()
                }
            }
            AttachOption(icon = DesignR.drawable.ic_file, label = stringResource(R.string.file), tint = Color(0xFF0E8577)) {
                fileLauncher.launch(arrayOf("*/*"))
            }
        }
    }
}

@Composable
private fun AttachOption(icon: Int, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(tint, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Text(text = label, color = SwiftTheme.colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** cacheDir/camera — FileProvider shu papkani ochib beradi (conversation_file_paths.xml). */
private fun newCameraFile(context: Context): File =
    File(File(context.cacheDir, "camera").apply { mkdirs() }, "IMG_${System.currentTimeMillis()}.jpg")
