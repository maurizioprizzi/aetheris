package org.aetheris.app.presentation.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val CAMERA_PERMISSION =
    Manifest.permission.CAMERA

@Composable
fun CameraPermissionHandler(
    onPermissionDeclined: (() -> Unit)? = null,
    onPermissionGranted: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            context.hasCameraPermission()
        )
    }

    /*
     * Sobrevive a mudanças de configuração,
     * como a rotação da tela.
     */
    var permissionWasRequested by rememberSaveable {
        mutableStateOf(false)
    }

    var permissionDeniedPermanently by rememberSaveable {
        mutableStateOf(false)
    }

    fun refreshPermissionState() {
        hasCameraPermission =
            context.hasCameraPermission()

        permissionDeniedPermanently =
            !hasCameraPermission &&
                    permissionWasRequested &&
                    activity != null &&
                    !ActivityCompat
                        .shouldShowRequestPermissionRationale(
                            activity,
                            CAMERA_PERMISSION
                        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasCameraPermission = granted

            permissionDeniedPermanently =
                !granted &&
                        activity != null &&
                        !ActivityCompat
                            .shouldShowRequestPermissionRationale(
                                activity,
                                CAMERA_PERMISSION
                            )
        }

    /*
     * Revalida a permissão quando o usuário retorna
     * da tela de configurações do aplicativo.
     */
    DisposableEffect(
        lifecycleOwner,
        activity
    ) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionState()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (hasCameraPermission) {
        onPermissionGranted()
    } else {
        CameraPermissionDeniedScreen(
            isPermanentlyDenied =
                permissionDeniedPermanently,
            onRequestPermission = {
                permissionWasRequested = true

                permissionLauncher.launch(
                    CAMERA_PERMISSION
                )
            },
            onOpenSettings = {
                context.openApplicationSettings()
            },
            onPermissionDeclined =
                onPermissionDeclined
        )
    }
}

@Composable
private fun CameraPermissionDeniedScreen(
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onPermissionDeclined: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "ACESSO À CÂMERA NECESSÁRIO",
                color = MaterialTheme.colorScheme.error,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = permissionExplanation(
                    isPermanentlyDenied =
                        isPermanentlyDenied
                ),
                color =
                    MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = {
                    if (isPermanentlyDenied) {
                        onOpenSettings()
                    } else {
                        onRequestPermission()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        MaterialTheme.colorScheme.primary,
                    contentColor =
                        MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = if (isPermanentlyDenied) {
                        "Abrir configurações"
                    } else {
                        "Permitir acesso à câmera"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            if (onPermissionDeclined != null) {
                OutlinedButton(
                    onClick = onPermissionDeclined
                ) {
                    Text(
                        text = "Agora não"
                    )
                }
            }
        }
    }
}

private fun permissionExplanation(
    isPermanentlyDenied: Boolean
): String {
    return if (isPermanentlyDenied) {
        "A permissão da câmera foi desativada. " +
                "Para usar a medição espacial, habilite " +
                "a câmera nas configurações do aplicativo."
    } else {
        "A câmera é necessária para mostrar o ambiente, " +
                "detectar superfícies e posicionar os " +
                "pontos da medição."
    }
}

private fun Context.hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        CAMERA_PERMISSION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun Context.openApplicationSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts(
            "package",
            packageName,
            null
        )
    ).apply {
        if (findActivity() == null) {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }
    }

    startActivity(intent)
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this

        is ContextWrapper ->
            baseContext.findActivity()

        else -> null
    }
}