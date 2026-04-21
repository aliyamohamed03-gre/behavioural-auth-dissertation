package uk.ac.gre.behaviouralauth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.ac.gre.behaviouralauth.model.AuthState
import uk.ac.gre.behaviouralauth.ui.theme.AppBackground
import uk.ac.gre.behaviouralauth.ui.theme.AppPrimary
import uk.ac.gre.behaviouralauth.ui.theme.AppSurface
import uk.ac.gre.behaviouralauth.ui.theme.SwipeAreaBackground
import uk.ac.gre.behaviouralauth.ui.theme.SwipeAreaBorder

@Composable
fun ScreenFrame(
    title: String,
    contentDescription: String = title,
    paddingValues: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(containerColor = AppBackground) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .semantics { this.contentDescription = contentDescription },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = text,
    disabledStateDescription: String? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics {
                this.contentDescription = contentDescription
                if (!enabled && disabledStateDescription != null) {
                    this.stateDescription = disabledStateDescription
                }
            },
        colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun StatusBanner(authState: AuthState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .semantics {
                contentDescription = "Authentication status: ${authState.label}"
                liveRegion = LiveRegionMode.Polite
            },
        colors = CardDefaults.cardColors(containerColor = authState.bannerBackground),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = authState.symbol,
                fontSize = 32.sp,
                color = authState.bannerText
            )
            Text(
                text = authState.label,
                style = MaterialTheme.typography.headlineSmall,
                color = authState.bannerText,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    contentDescription: String = title ?: "Section",
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

@Composable
fun SwipeCaptureArea(
    onGestureCaptured: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Swipe area. Perform swipe gestures here to record your gesture pattern."
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SwipeAreaBackground)
            .drawBehind {
                drawRoundRect(
                    color = SwipeAreaBorder,
                    cornerRadius = CornerRadius(32f, 32f),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 12f), 0f)
                    )
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onGestureCaptured() },
                    onDrag = { change, _ -> change.consume() }
                )
            }
            .semantics {
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Swipe here in different directions",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF1E3A5F),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
