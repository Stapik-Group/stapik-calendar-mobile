package pl.stapik.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import pl.stapik.calendar.ui.calendar.WeekPagerScreen
import pl.stapik.calendar.ui.theme.RetroColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = RetroColors.WindowBackground) {
                WeekPagerScreen()
            }
        }
    }
}