package com.jakewharton.mosaic

import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal interface Output {
	fun display(content: String, static: String?)
}

@OptIn(ExperimentalTime::class) // Not used in production.
internal object DebugOutput : Output {
	private val systemClock = TimeSource.Monotonic
	private var lastRender: TimeMark? = null

	override fun display(content: String, static: String?) {
		println(buildString {
			lastRender?.let { lastRender ->
				appendLine()
				repeat(50) { append('~') }
				append(" +")
				appendLine(lastRender.elapsedNow())
			}
			lastRender = systemClock.markNow()

			if (static != null) {
				appendLine(static)
			}
			appendLine(content)
		})
	}
}

internal object AnsiOutput : Output {
	private val stringBuilder = StringBuilder(100)
	private var lastHeight = 0

	override fun display(content: String, static: String?) {
		stringBuilder.apply {
			clear()

			repeat(lastHeight) {
				append("\u001B[F") // Cursor up line.
			}

			val staticLines = static?.split("\n") ?: emptyList()
			val lines = content.split("\n")
			for (line in staticLines + lines) {
				append(line)
				append("\u001B[K") // Clear rest of line.
				append('\n')
			}

			// If the new output contains fewer lines than the last output, clear those old lines.
			val extraLines = lastHeight - lines.size
			for (i in 0 until extraLines) {
				if (i > 0) {
					append('\n')
				}
				append("\u001B[K") // Clear line.
			}

			// Move cursor back up to end of the new output.
			repeat(extraLines - 1) {
				append("\u001B[F") // Cursor up line.
			}

			lastHeight = lines.size
		}

		platformRender(stringBuilder)
	}
}
