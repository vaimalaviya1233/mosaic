package com.jakewharton.mosaic

import androidx.compose.runtime.AbstractApplier
import com.facebook.yoga.YogaMeasureOutput
import com.facebook.yoga.YogaNode
import com.facebook.yoga.YogaNodeFactory
import com.jakewharton.crossword.TextCanvas
import com.jakewharton.crossword.visualCodePointCount

internal interface Renderable {
	fun render(canvas: TextCanvas)
}

internal class RenderableLink(
	val self: Renderable,
	val next: Renderable?,
) : Renderable {
	override fun render(canvas: TextCanvas) {
		self.render(canvas)
		next?.render(canvas)
	}
}

class BackgroundModifier : Modifier.Element {
	override fun toString() = "Background(TODO)"
}

class BackgroundRenderable : Renderable {
	override fun render(canvas: TextCanvas) {
		TODO("Not yet implemented")
	}
}

internal sealed class MosaicNode : Renderable {
	val yoga: YogaNode = YogaNodeFactory.create()
}

internal class TextNode(initialValue: String = "") : MosaicNode() {
	init {
		yoga.setMeasureFunction { _, _, _, _, _ ->
			val lines = value.split('\n')
			val measuredWidth = lines.maxOf { it.visualCodePointCount }
			val measuredHeight = lines.size
			YogaMeasureOutput.make(measuredWidth, measuredHeight)
		}
	}

	var value: String = initialValue
		set(value) {
			field = value
			yoga.dirty()
		}

	private val innerRenderable = object : Renderable {
		override fun render(canvas: TextCanvas) {
			value.split('\n').forEachIndexed { index, line ->
				canvas.write(index, 0, line)
			}
		}
	}

	private var renderable: Renderable = innerRenderable

	fun setModifiers(modifier: Modifier) {
		renderable = modifier.foldOut<Renderable>(innerRenderable) { element, p ->
			val thisRenderable = when (element) {
				is BackgroundModifier -> BackgroundRenderable()
				else -> throw IllegalStateException("Unknown modifier element")
			}
			RenderableLink(thisRenderable, p)
		}
	}

	override fun render(canvas: TextCanvas) {
		renderable.render(canvas)
	}

	override fun toString() = "Text($value)"
}

internal class BoxNode : MosaicNode() {
	val children = mutableListOf<MosaicNode>()

	override fun render(canvas: TextCanvas) {
		for (child in children) {
			val childYoga = child.yoga
			val left = childYoga.layoutX.toInt()
			val top = childYoga.layoutY.toInt()
			val right = left + childYoga.layoutWidth.toInt()
			val bottom = top + childYoga.layoutHeight.toInt()
			val clipped = canvas.clip(left, top, right, bottom)
			child.render(clipped)
		}
	}

	override fun toString() = children.joinToString(prefix = "Box(", postfix = ")")
}

internal class MosaicNodeApplier(root: BoxNode) : AbstractApplier<MosaicNode>(root) {
	override fun insert(index: Int, instance: MosaicNode) {
		val boxNode = current as BoxNode
		boxNode.children.add(index, instance)
		boxNode.yoga.addChildAt(instance.yoga, index)
	}

	override fun remove(index: Int, count: Int) {
		val boxNode = current as BoxNode
		boxNode.children.remove(index, count)
		repeat(count) {
			boxNode.yoga.removeChildAt(index)
		}
	}

	override fun move(from: Int, to: Int, count: Int) {
		val boxNode = current as BoxNode
		boxNode.children.move(from, to, count)

		val yoga = boxNode.yoga
		val newIndex = if (to > from) to - count else to
		if (count == 1) {
			val node = yoga.removeChildAt(from)
			yoga.addChildAt(node, newIndex)
		} else {
			val nodes = Array(count) {
				yoga.removeChildAt(from)
			}
			nodes.forEachIndexed { offset, node ->
				yoga.addChildAt(node, newIndex + offset)
			}
		}
	}

	override fun onClear() {
		val boxNode = root as BoxNode
		// Remove in reverse to avoid internal list copies.
		for (i in boxNode.yoga.childCount - 1 downTo 0) {
			boxNode.yoga.removeChildAt(i)
		}
	}
}
