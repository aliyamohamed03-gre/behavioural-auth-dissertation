package uk.ac.gre.behaviouralauth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import uk.ac.gre.behaviouralauth.ui.theme.BehaviouralAuthTheme

//Main Android activity that launches the Compose-based behavioural authentication app.
class MainActivity : ComponentActivity() {

    //Sets up edge-to-edge display and loads the app UI when the activity starts.
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