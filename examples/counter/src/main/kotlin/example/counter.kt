package example

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jakewharton.mosaic.Text
import com.jakewharton.mosaic.runMosaic
import kotlinx.coroutines.delay

fun main() = runMosaic {
	var count by remember { mutableStateOf(0) }
	Text("The count is: $count")

	LaunchedEffect(Unit) {
		for (i in 1..20) {
			delay(250)
			count = i
		}
	}
}
