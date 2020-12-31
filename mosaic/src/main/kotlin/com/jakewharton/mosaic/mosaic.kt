package com.jakewharton.mosaic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.EmbeddingContext
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.compositionFor
import androidx.compose.runtime.dispatch.BroadcastFrameClock
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.yoloGlobalEmbeddingContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * True when using ANSI control sequences to overwrite output.
 * False for a debug-like output that renders each "frame" on its own with a timestamp delta.
 */
private const val ansiConsole = true

fun runMosaic(body: @Composable () -> Unit) = runBlocking {
	val output = if (ansiConsole) AnsiOutput else DebugOutput

	var hasFrameWaiters = false
	val clock = BroadcastFrameClock {
		hasFrameWaiters = true
	}

	val compositionJob = Job(coroutineContext[Job])
	val composeContext = coroutineContext + clock + compositionJob
	val mainThread = Thread.currentThread()
	yoloGlobalEmbeddingContext = object : EmbeddingContext {
		override fun isMainThread() = Thread.currentThread() === mainThread
		override fun mainThreadCompositionContext() = composeContext
	}

	var snapshotNotificationsPending = false
	val observer: (state: Any) -> Unit = {
		if (!snapshotNotificationsPending) {
			snapshotNotificationsPending = true
			launch {
				snapshotNotificationsPending = false
				Snapshot.sendApplyNotifications()
			}
		}
	}
	Snapshot.registerGlobalWriteObserver(observer)

	val rootNode = BoxNode()
	var displaySignal: CompletableDeferred<Unit>? = null
	launch(context = composeContext) {
		while (true) {
			if (hasFrameWaiters) {
				hasFrameWaiters = false
				clock.sendFrame(0L) // Frame time value is not used by Compose runtime.

				output.display(rootNode.render())
				displaySignal?.complete(Unit)
			}
			delay(50)
		}
	}

	val composition: Composition
	coroutineScope {
		val effectScope = this

		val recomposer = Recomposer(effectScope.coroutineContext)
		composition = compositionFor(Any(), MosaicNodeApplier(rootNode), recomposer)

		// Start undispatched to ensure we can use suspending things inside the content.
		launch(start = UNDISPATCHED, context = composeContext) {
			recomposer.runRecomposeAndApplyChanges()
		}

		val myJob = coroutineContext[Job]!!
		myJob.children.single().invokeOnCompletion {
			repeat(10) {
				println(2)
			}
		}
		myJob.invokeOnCompletion {
			repeat(10) {
				println(1)
			}
		}

		composition.setContent(body)
		hasFrameWaiters = true

		println("X")
	}
	println("Y")

	// We know that Compose will create a child job on our effect context. For reasons I don't
	// understand, completing that child job
//	val internalEffectJob = effectJob.children.single() as CompletableJob
//	internalEffectJob.complete()

	// Ensure the final state modification is discovered. We need to ensure that the coroutine
	// which is running the recomposition loop wakes up, notices the changes, and waits for the
	// next frame. If you are using snapshots this only requires a single yield. If you are not
	// then it requires two yields. THIS IS NOT GREAT! But at least it's implementation detail...
	// TODO https://issuetracker.google.com/issues/169425431
//	yield()
//	yield()
//	Snapshot.sendApplyNotifications()
//	yield()
//	yield()

//	internalEffectJob.join()
//	effectJob.cancel()

//	if (hasFrameWaiters) {
//		CompletableDeferred<Unit>().also {
//			displaySignal = it
//			it.await()
//		}
//	}

	compositionJob.cancel()
	composition.dispose()
}
