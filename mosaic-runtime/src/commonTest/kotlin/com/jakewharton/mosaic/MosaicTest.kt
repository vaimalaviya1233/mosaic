package com.jakewharton.mosaic

import kotlin.test.Test
import kotlin.test.assertEquals

class MosaicTest {
	private val esc = "\u001B"

	@Test fun render() {
		val actual = renderMosaic {
			Column {
				Text("One")
				Text("Two")
				Text("Three")
			}
		}
		assertEquals("""
			|One  $esc[K
			|Two  $esc[K
			|Three$esc[K
			|""".trimMargin(), actual)
	}
}