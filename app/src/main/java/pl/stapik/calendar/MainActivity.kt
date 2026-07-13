package pl.stapik.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import pl.stapik.calendar.data.config.DataStoreApiConfigStorage
import pl.stapik.calendar.ui.root.AppRoot
import pl.stapik.calendar.ui.theme.RetroColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val apiConfigStorage = remember { DataStoreApiConfigStorage(applicationContext) }
            Surface(modifier = Modifier.fillMaxSize(), color = RetroColors.WindowBackground) {
                AppRoot(apiConfigStorage = apiConfigStorage)
            }
        }
    }
}
