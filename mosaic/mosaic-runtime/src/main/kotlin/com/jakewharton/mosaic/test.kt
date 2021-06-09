package com.jakewharton.mosaic

import androidx.constraintlayout.core.widgets.ConstraintAnchor.Type
import androidx.constraintlayout.core.widgets.ConstraintWidget
import androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.FIXED
import androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.MATCH_PARENT
import androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.WRAP_CONTENT
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer
import androidx.constraintlayout.core.widgets.Flow
import androidx.constraintlayout.core.widgets.Optimizer.OPTIMIZATION_NONE
import androidx.constraintlayout.core.widgets.VirtualLayout
import androidx.constraintlayout.core.widgets.analyzer.BasicMeasure
import androidx.constraintlayout.core.widgets.analyzer.BasicMeasure.EXACTLY
import androidx.constraintlayout.core.widgets.analyzer.BasicMeasure.UNSPECIFIED

fun main() {
	val container = ConstraintWidgetContainer()
	container.measurer = Measurer
	container.debugName = "container"
	container.horizontalDimensionBehaviour = WRAP_CONTENT
	container.verticalDimensionBehaviour = WRAP_CONTENT

	val column = Flow()
	column.debugName = "column"
	column.setOrientation(Flow.VERTICAL)
	column.horizontalDimensionBehaviour = WRAP_CONTENT
	column.verticalDimensionBehaviour = WRAP_CONTENT
//	column.setHorizontalAlign(Flow.HORIZONTAL_ALIGN_START) // <<---- FIXES THE PROBLEM
	column.connect(Type.TOP, container, Type.TOP)
	column.connect(Type.BOTTOM, container, Type.BOTTOM)
	container.add(column)

	val row1 = Flow()
	row1.debugName = "row1"
	row1.setOrientation(Flow.HORIZONTAL)
	row1.horizontalDimensionBehaviour = WRAP_CONTENT
	row1.verticalDimensionBehaviour = WRAP_CONTENT
	row1.connect(Type.LEFT, column, Type.LEFT)
	row1.connect(Type.RIGHT, column, Type.RIGHT)
	column.add(row1)
	container.add(row1)

	val hello = ConstraintWidget(7, 1)
	hello.debugName = "Hello, "
	row1.add(hello)
	container.add(hello)

	val world = ConstraintWidget(6, 1)
	world.debugName = "World!"
	row1.add(world)
	container.add(world)

	val row2 = Flow()
	row2.debugName = "row2"
	row2.setOrientation(Flow.HORIZONTAL)
	row2.horizontalDimensionBehaviour = WRAP_CONTENT
	row2.verticalDimensionBehaviour = WRAP_CONTENT
	row2.connect(Type.LEFT, column, Type.LEFT)
	row2.connect(Type.RIGHT, column, Type.RIGHT)
	column.add(row2)
	container.add(row2)

	val its = ConstraintWidget(5, 1)
	its.debugName = "It's "
	row2.add(its)
	container.add(its)

	val constraints = ConstraintWidget(12, 1)
	constraints.debugName = "Constraints!"
	row2.add(constraints)
	container.add(constraints)

	container.measure(OPTIMIZATION_NONE, 0, 0, 0 ,0, 0, 0, 0, 0)
	container.layout()

	println(container)
	println(column)
	println(row1)
	println(hello)
	println(world)
	println(row2)
	println(its)
	println(constraints)
}

object Measurer : BasicMeasure.Measurer {
	override fun measure(widget: ConstraintWidget, measure: BasicMeasure.Measure) {
		val horizontalBehavior = measure.horizontalBehavior
		val verticalBehavior = measure.verticalBehavior
		val horizontalDimension = measure.horizontalDimension
		val verticalDimension = measure.verticalDimension

		if (widget is VirtualLayout) {
			var widthMode: Int = UNSPECIFIED
			var heightMode: Int = UNSPECIFIED
			var widthSize = 0
			var heightSize = 0
			if (widget.horizontalDimensionBehaviour == MATCH_PARENT) {
				widthSize = widget.parent?.width ?: 0
				widthMode = EXACTLY
			} else if (horizontalBehavior == FIXED) {
				widthSize = horizontalDimension
				widthMode = EXACTLY
			}
			if (widget.getVerticalDimensionBehaviour() == MATCH_PARENT) {
				heightSize = widget.parent?.height ?: 0
				heightMode = EXACTLY
			} else if (verticalBehavior == FIXED) {
				heightSize = verticalDimension
				heightMode = EXACTLY
			}
			widget.measure(widthMode, widthSize, heightMode, heightSize)
			measure.measuredWidth = widget.measuredWidth
			measure.measuredHeight = widget.measuredHeight
		} else {
			measure.measuredWidth = widget.width
			measure.measuredHeight = widget.height
		}
	}

	override fun didMeasures() {
	}
}
