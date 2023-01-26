package com.jakewharton.mosaic

import androidx.compose.runtime.AbstractApplier
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.table.LinearLayoutBuilder
import com.github.ajalt.mordant.table.horizontalLayout
import com.github.ajalt.mordant.table.verticalLayout
import com.github.ajalt.mordant.widgets.Text

internal sealed interface MosaicNode {
	fun toWidget(): Widget
	fun staticWidgets(): List<Widget>
}

internal class TextNode(var text: String = "") : MosaicNode {
	var foreground: Color? = null
	var background: Color? = null
	var style: TextStyle? = null

	override fun staticWidgets() = emptyList<Widget>()
	override fun toWidget() = Text(text)
	override fun toString() = "Text($text)"
}

internal sealed interface ContainerNode : MosaicNode {
	val children: MutableList<MosaicNode>
}

internal class LinearNode(var isRow: Boolean = true) : ContainerNode {
	override val children = mutableListOf<MosaicNode>()

	override fun toWidget(): Widget {
		fun LinearLayoutBuilder.addChildren() {
			for (child in children) {
				cell(child.toWidget())
			}
		}
		return if (isRow) {
			horizontalLayout { addChildren() }
		} else {
			verticalLayout { addChildren() }
		}
	}

	override fun staticWidgets(): List<Widget> {
		return children.flatMap(MosaicNode::staticWidgets)
	}

	override fun toString() = children.joinToString(prefix = "Box(", postfix = ")")
}

internal class MosaicNodeApplier(root: LinearNode) : AbstractApplier<MosaicNode>(root) {
	override fun insertTopDown(index: Int, instance: MosaicNode) {
		// Ignored, we insert bottom-up.
	}

	override fun insertBottomUp(index: Int, instance: MosaicNode) {
		val boxNode = current as ContainerNode
		boxNode.children.add(index, instance)
	}

	override fun remove(index: Int, count: Int) {
		val boxNode = current as ContainerNode
		boxNode.children.remove(index, count)
	}

	override fun move(from: Int, to: Int, count: Int) {
		val boxNode = current as ContainerNode
		boxNode.children.move(from, to, count)
	}

	override fun onClear() {
		val boxNode = root as ContainerNode
		boxNode.children.clear()
	}
}
