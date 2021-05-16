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
	container.debugName = "container"
	container.horizontalDimensionBehaviour = WRAP_CONTENT
	container.verticalDimensionBehaviour = WRAP_CONTENT
	container.measurer = object : BasicMeasure.Measurer {
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

	val row = Flow()
	row.debugName = "row"
	row.setOrientation(Flow.HORIZONTAL)
	row.horizontalDimensionBehaviour = WRAP_CONTENT
	row.verticalDimensionBehaviour = WRAP_CONTENT
	row.connect(Type.LEFT, container, Type.LEFT)
	row.connect(Type.RIGHT, container, Type.RIGHT)
	container.add(row)

	val hello = ConstraintWidget(7, 1)
	hello.debugName = "Hello, "
	row.add(hello)
	container.add(hello)

	val world = ConstraintWidget(6, 1)
	world.debugName = "World!"
	row.add(world)
	container.add(world)

	container.measure(OPTIMIZATION_NONE, 0, 0, 0 ,0, 0, 0, 0, 0)
	container.layout()

	println(container)
	println(row)
	println(hello)
	println(world)
}
