package uk.ac.gre.behaviouralauth.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BehaviouralAuthColorScheme = lightColorScheme(
    primary = AppPrimary,
    onPrimary = AppOnPrimary,
    background = AppBackground,
    surface = AppSurface
)

@Composable
fun BehaviouralAuthTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BehaviouralAuthColorScheme,
        typography = AppTypography,
        content = content
    )
}

