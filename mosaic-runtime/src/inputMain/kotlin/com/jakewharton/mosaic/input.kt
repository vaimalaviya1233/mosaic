package com.jakewharton.mosaic

import com.github.ajalt.mordant.input.MouseTracking
import com.github.ajalt.mordant.input.RawModeScope
import com.github.ajalt.mordant.input.enterRawModeOrNull
import com.github.ajalt.mordant.terminal.Terminal
import kotlin.time.Duration

internal actual fun Terminal.enterRawMode(): RawMode? {
	return enterRawModeOrNull(MouseTracking.Off)
		?.let(::MordantRawMode)
}

private class MordantRawMode(
	private val delegate: RawModeScope,
) : RawMode,
	AutoCloseable by delegate {
	override fun readKey(timeout: Duration) = delegate.readKeyOrNull(timeout)
}
