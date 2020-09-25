package com.jakewharton.mosaic

import androidx.compose.runtime.Composable

@Suppress("FunctionName") // Type constructor.
fun TestMosaic(
	ansi: Boolean = true,
	content: @Composable () -> Unit,
): TestMosaic {
	TODO()
}

interface TestMosaic {
	suspend fun awaitRender(): String
}

private class TestMosaicImpl : TestMosaic {
	override suspend fun awaitRender(): String {
		TODO("Not yet implemented")
	}
}
