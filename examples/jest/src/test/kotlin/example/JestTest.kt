package example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import com.google.common.truth.Truth.assertThat
import example.TestState.Pass
import example.TestState.Running
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

class JestTest {
	@Test fun noAnsi() = runBlockingTest {
		val testRuns = mutableStateListOf<TestRun>()
		val mosaic = TestMosaic(ansi = false) {
			JestOutput(testRuns)
		}

		assertThat(mosaic.awaitRender()).isEqualTo("""
			|Tests: 0 total
			|Time:  0s
			""".trimMargin())

		testRuns += TestRun("example/foo.kt", Running)
		assertThat(mosaic.awaitRender()).isEqualTo("""
			| RUNS  example/foo.kt
			|
			|Tests: 1 total
			|Time:  0s
			""".trimMargin())

		advanceTimeBy(1_000)
		assertThat(mosaic.awaitRender()).isEqualTo("""
			| RUNS  example/foo.kt
			|
			|Tests: 1 total
			|Time:  1s
			""".trimMargin())

		testRuns[0] = testRuns[0].copy(state = Pass)
		assertThat(mosaic.awaitRender()).isEqualTo("""
			| PASS  example/foo.kt
			|
			|Tests: 1 passed, 1 total
			|Time:  1s
			""".trimMargin())
	}
}
