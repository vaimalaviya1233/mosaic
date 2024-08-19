package com.jakewharton.mosaic

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.terminal.Terminal
import kotlin.time.Duration

internal interface RawMode : AutoCloseable {
	fun readKey(timeout: Duration): KeyboardEvent?
}

internal expect fun Terminal.enterRawMode(): RawMode?
