package com.jakewharton.mosaic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.github.ajalt.mordant.rendering.Lines
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.rendering.WidthRange
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.flow.Flow

/**
 * Will render each value emitted by [items] as permanent output above the
 * regular display.
 */
@Composable
public fun <T> Static(
	items: Flow<T>,
	content: @Composable (T) -> Unit,
) {
	class Item(val value: T, var rendered: Boolean)

	// Keep list of items which have not yet been rendered.
	val pending = remember { mutableStateListOf<Item>() }

	LaunchedEffect(items) {
		items.collect {
			pending.add(Item(it, rendered = false))
		}
	}

	ComposeNode<StaticNode, MosaicNodeApplier>(
		factory = {
			StaticNode {
				pending.removeAll { it.rendered }
			}
		},
		update = {},
		content = {
			for (item in pending) {
				Row {
					// Render item and mark it as having been included in render.
					content(item.value)
					item.rendered = true
				}
			}
		},
	)
}

internal class StaticNode(
	private val postRender: () -> Unit,
) : ContainerNode, Widget {
	// Delegate container column for static content.
	private val column = LinearNode(isRow = false)

	override val children: MutableList<MosaicNode>
		get() = column.children

	override fun staticWidgets(): List<Widget> {
		val statics = buildList {
			if (column.children.isNotEmpty()) {
				add(column.toWidget())
			}
			addAll(column.staticWidgets())
		}

		postRender()

		return statics
	}

	override fun toWidget() = this
	override fun measure(t: Terminal, width: Int) = WidthRange(0, 0)
	override fun render(t: Terminal, width: Int) = Lines(emptyList())

	override fun toString() = column.children.joinToString(prefix = "Static(", postfix = ")")
}
