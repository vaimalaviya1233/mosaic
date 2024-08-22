package com.jakewharton.mosaic

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import platform.posix.SIGABRT
import platform.posix.SIGALRM
import platform.posix.SIGBUS
import platform.posix.SIGFPE
import platform.posix.SIGHUP
import platform.posix.SIGINT
import platform.posix.SIGQUIT
import platform.posix.SIGTERM
import platform.posix.SIG_DFL
import platform.posix.atexit
import platform.posix.getpid
import platform.posix.kill
import platform.posix.signal

// TODO thread safety
private val shutdownHooks = mutableListOf<() -> Unit>()

private fun runShutdownHooks() {
	for (shutdownHook in shutdownHooks) {
		shutdownHook()
	}
}

@OptIn(ExperimentalForeignApi::class)
private fun signalHandler(value: Int) {
	runShutdownHooks()

	// Restore the default handler and re-send the signal back to ourselves. Per signal(7)
	// it is safe to use signal here instead of sigaction.
	signal(value, SIG_DFL)
	kill(getpid(), value)
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun addPlatformShutdownHook(block: () -> Unit) {
	val addSignalHandler = shutdownHooks.isEmpty()

	shutdownHooks += block

	if (addSignalHandler) {
		atexit(staticCFunction(::runShutdownHooks))

		// TODO Migrate to sigaction.
		val signalHandlerFunction = staticCFunction(::signalHandler)
		signal(SIGABRT, signalHandlerFunction)
		signal(SIGALRM, signalHandlerFunction)
		signal(SIGBUS, signalHandlerFunction)
		signal(SIGFPE, signalHandlerFunction)
		signal(SIGHUP, signalHandlerFunction)
		signal(SIGINT, signalHandlerFunction)
		signal(SIGTERM, signalHandlerFunction)
		signal(SIGQUIT, signalHandlerFunction)
	}
}
