package com.jakewharton.mosaic

private fun importNodeProcess(): NodeJsProcess = js("""require("process")""")

private external interface NodeJsProcess {
	fun on(signal: String, block: () -> Unit)
}

internal actual fun addPlatformShutdownHook(block: () -> Unit) {
	importNodeProcess().apply {
		on("exit", block)
		on("SIGINT", block)
	}
}
