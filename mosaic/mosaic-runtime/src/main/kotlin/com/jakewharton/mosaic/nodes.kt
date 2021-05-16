package com.jakewharton.mosaic

import androidx.compose.runtime.AbstractApplier
import androidx.constraintlayout.core.widgets.ConstraintWidget
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer
import androidx.constraintlayout.core.widgets.Flow

internal interface Node {
	fun renderTo(canvas: TextCanvas)
}

internal class TextNode : ConstraintWidget(), Node {
	var value: String = ""
		set(value) {
			field = value

			val lines = value.split('\n')
			width = lines.maxOf { it.codePointCount(0, it.length) }
			height = lines.size
		}

	var foreground: Color? = null
	var background: Color? = null
	var style: TextStyle? = null

	override fun renderTo(canvas: TextCanvas) {
		value.split('\n').forEachIndexed { index, line ->
			canvas.write(index, 0, line, foreground, background, style)
		}
	}
}

internal class BoxNode : Flow(), Node {
	override fun renderTo(canvas: TextCanvas) {
		for (child in mWidgets) {
			val clipped = canvas[child.top until child.bottom, child.left until child.right]
			(child as Node).renderTo(clipped)
		}
	}
}

internal class MosaicNodeApplier(
	private val container: ConstraintWidgetContainer,
	root: BoxNode,
) : AbstractApplier<ConstraintWidget>(root) {
	override fun insertTopDown(index: Int, instance: ConstraintWidget) {
		container.add(instance)

		val boxNode = current as BoxNode
		var widgets = boxNode.mWidgets
		val widgetCount = boxNode.mWidgetsCount

		// Check for require expansion.
		if (widgetCount == widgets.size) {
			widgets = widgets.copyOf(widgetCount * 2)
			boxNode.mWidgets = widgets
		}

		// Check for required element shift to accomodate insertion in the middle.
		if (index < widgetCount) {
			widgets.copyInto(widgets, index + 1, index, widgetCount)
		}

		widgets[index] = instance
		boxNode.mWidgetsCount = widgetCount + 1
	}

	override fun insertBottomUp(index: Int, instance: ConstraintWidget) {
		// This applier inserts top-down.
	}

	override fun remove(index: Int, count: Int) {
		val boxNode = current as BoxNode
		val widgets = boxNode.mWidgets
		val widgetCount = boxNode.mWidgetsCount

		val endIndex = index + count
		for (i in index until endIndex) {
			container.remove(widgets[i])
		}

		val clearFromIndex = if (endIndex == widgetCount) {
			index
		} else {
			widgets.copyInto(widgets, index, endIndex, widgetCount)
			widgetCount - count
		}
		widgets.fill(null, clearFromIndex)
		boxNode.mWidgetsCount = widgetCount - count
	}

	override fun move(from: Int, to: Int, count: Int) {
		val boxNode = current as BoxNode
		val widgets = boxNode.mWidgets

		if (count == 1) {
			val item = widgets[from]
			// TODO shift other elements
			// TODO write item
			TODO()
		} else {
			// TODO copy elements into array
			// TODO shift other elements
			// TODO write element array
			TODO()
		}
	}

	override fun onClear() {
		container.removeAllChildren()
		container.add(root)

		val boxNode = current as BoxNode
		boxNode.removeAllIds()
	}
}
