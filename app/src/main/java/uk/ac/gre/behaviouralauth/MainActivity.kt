package uk.ac.gre.behaviouralauth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import uk.ac.gre.behaviouralauth.ui.theme.BehaviouralAuthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BehaviouralAuthTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BehaviouralAuthApp()
                }
            }
        }
    }
}

