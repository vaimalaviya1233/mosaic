package example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.launchInComposition
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.withMutableSnapshot
import com.jakewharton.mosaic.Column
import com.jakewharton.mosaic.Row
import com.jakewharton.mosaic.Text
import com.jakewharton.mosaic.launchMosaic
import example.TestState.Fail
import example.TestState.Pass
import example.TestState.Running
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.ansi
import kotlin.random.Random

fun main() = runBlocking {
	val testRuns = mutableStateListOf<TestRun>()

	val job = launchMosaic {
		JestOutput(testRuns)
	}

	// This scope is the "test runner" which interacts with the UI solely through 'tests'.
	coroutineScope {
		val paths = ArrayDeque(listOf(
			"tests/login.kt",
			"tests/signup.kt",
			"tests/forgot-password.kt",
			"tests/reset-password.kt",
			"tests/view-profile.kt",
			"tests/edit-profile.kt",
			"tests/delete-profile.kt",
			"tests/posts.kt",
			"tests/post.kt",
			"tests/comments.kt",
		))
		repeat(4) { // Number of test workers.
			launch {
				while (true) {
					val path = paths.removeFirstOrNull() ?: break
					val index = withMutableSnapshot {
						val nextIndex = testRuns.size
						testRuns += TestRun(path, Running)
						nextIndex
					}
					delay(Random.nextLong(2_000L, 4_000L))
					withMutableSnapshot {
						// Flip a coin biased 60% to pass to produce the final state of the test.
						val newState = if (Random.nextFloat() < .6f) Pass else Fail
						testRuns[index] = testRuns[index].copy(state = newState)
					}
				}
			}
		}
	}

	// TODO how do we wait for the final frame?
	delay(200) // HACK!

	job.cancel()
}

@Composable
internal fun JestOutput(testRuns: SnapshotStateList<TestRun>) {
	val (done, running) = testRuns.partition { it.state != Running }
	Column {
		if (done.isNotEmpty()) {
			for (test in done) {
				TestRow(test)
			}
			Text("") // Blank line
		}

		if (running.isNotEmpty()) {
			for (test in running) {
				TestRow(test)
			}
			Text("") // Blank line
		}

		Summary(testRuns)
	}
}

@Composable
private fun TestRow(testRun: TestRun) {
	val bg = when (testRun.state) {
		Running -> Ansi.Color.YELLOW
		Pass -> Ansi.Color.GREEN
		Fail -> Ansi.Color.RED
	}
	val state = when (testRun.state) {
		Running -> "RUNS"
		Pass -> "PASS"
		Fail -> "FAIL"
	}
	val dir = testRun.path.substringBeforeLast('/')
	val name = testRun.path.substringAfterLast('/')
	Text(ansi()
		.bg(bg).fgBlack().a(' ').a(state).a(' ').reset()
		.a(' ')
		.a(dir).a('/').fgBrightDefault().bold().a(name).reset()
		.toString())
}

@Composable
private fun Summary(testRuns: SnapshotStateList<TestRun>) {
	Row {
		Text("Tests: ")

		val failed = testRuns.count { it.state == Fail }
		if (failed > 0) {
			Text(ansi()
				.fgRed().a(failed).a(" failed").reset()
				.a(", ")
				.toString())
		}

		val passed = testRuns.count { it.state == Pass }
		if (passed > 0) {
			Text(ansi()
				.fgGreen().a(passed).a(" passed").reset()
				.a(", ")
				.toString())
		}

		Text("${testRuns.size} total")
	}

	var elapsed by remember { mutableStateOf(0) }
	launchInComposition {
		while (true) {
			delay(1_000)
			elapsed++
		}
	}
	Text("Time:  ${elapsed}s")
}

data class TestRun(
	val path: String,
	val state: TestState,
)

enum class TestState {
	Running,
	Pass,
	Fail,
}
