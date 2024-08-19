package com.jakewharton.mosaic

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import com.github.ajalt.mordant.terminal.Terminal as MordantTerminal
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.layout.MosaicNode
import com.jakewharton.mosaic.ui.AnsiLevel
import com.jakewharton.mosaic.ui.BoxMeasurePolicy
import com.jakewharton.mosaic.ui.unit.IntSize
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * True for a debug-like output that renders each "frame" on its own with a timestamp delta.
 * False when using ANSI control sequences to overwrite output.
 */
private const val debugOutput = false

internal fun renderMosaicNode(content: @Composable () -> Unit): MosaicNode {
	val mosaicComposition = MosaicComposition(
		coroutineScope = CoroutineScope(EmptyCoroutineContext),
		terminalState = MordantTerminal().toMutableState(),
		keyEvents = Channel(),
		onEndChanges = {},
	)
	mosaicComposition.setContent(content)
	mosaicComposition.cancel()
	return mosaicComposition.rootNode
}

public fun renderMosaic(content: @Composable () -> Unit): String {
	return createRendering().render(renderMosaicNode(content)).toString()
}

public suspend fun runMosaic(content: @Composable () -> Unit) {
	coroutineScope {
		val terminal = MordantTerminal()
		val rendering = createRendering(terminal.info.ansiLevel.toMosaicAnsiLevel())
		val terminalState = terminal.toMutableState()
		val keyEvents = Channel<KeyEvent>(UNLIMITED)
		val mosaicComposition = MosaicComposition(
			coroutineScope = this,
			terminalState = terminalState,
			keyEvents = keyEvents,
			onEndChanges = { rootNode ->
				platformDisplay(rendering.render(rootNode))
			},
		)
		mosaicComposition.sendFrames()
		mosaicComposition.scope.updateTerminalInfo(terminal, terminalState)
		mosaicComposition.scope.enterRawModeAndReadKeys(terminal, keyEvents)
		mosaicComposition.setContent(content)
		mosaicComposition.awaitComplete()
	}
}

private fun MordantTerminal.toMutableState(): MutableState<Terminal> {
	return mutableStateOf(
		Terminal(size = IntSize(info.width, info.height)),
	)
}

private fun createRendering(ansiLevel: AnsiLevel = AnsiLevel.TRUECOLOR): Rendering {
	return if (debugOutput) {
		@OptIn(ExperimentalTime::class) // Not used in production.
		DebugRendering(ansiLevel = ansiLevel)
	} else {
		AnsiRendering(ansiLevel = ansiLevel)
	}
}

private fun CoroutineScope.updateTerminalInfo(terminal: MordantTerminal, terminalInfo: MutableState<Terminal>) {
	launch {
		while (true) {
			val currentTerminalInfo = terminalInfo.value
			if (terminal.info.updateTerminalSize() &&
				(
					currentTerminalInfo.size.width != terminal.info.width ||
						currentTerminalInfo.size.height != terminal.info.height
					)
			) {
				terminalInfo.value = Terminal(size = IntSize(terminal.info.width, terminal.info.height))
			}
			delay(50L)
		}
	}
}

private fun CoroutineScope.enterRawModeAndReadKeys(terminal: MordantTerminal, keyEvents: Channel<KeyEvent>) {
	terminal.enterRawMode()?.let { rawMode ->
		launch(Dispatchers.Default) {
			try {
				while (isActive) {
					val keyboardEvent = rawMode.readKey(10.milliseconds) ?: continue
					val keyEvent = KeyEvent(
						key = keyboardEvent.key,
						alt = keyboardEvent.alt,
						ctrl = keyboardEvent.ctrl,
						shift = keyboardEvent.shift,
					)
					keyEvents.trySend(keyEvent)
				}
			} finally {
				rawMode.close()
			}
		}
	}
}

internal class MosaicComposition(
	coroutineScope: CoroutineScope,
	private val terminalState: State<Terminal>,
	private val keyEvents: ReceiveChannel<KeyEvent>,
	onEndChanges: (MosaicNode) -> Unit,
) {
	private val job = Job(coroutineScope.coroutineContext[Job])
	private val clock = BroadcastFrameClock()
	private val composeContext: CoroutineContext = coroutineScope.coroutineContext + job + clock
	val scope = CoroutineScope(composeContext)

	private val applier = MosaicNodeApplier(onEndChanges)
	val rootNode = applier.root
	private val recomposer = Recomposer(composeContext)
	private val composition = Composition(applier, recomposer)

	init {
		GlobalSnapshotManager().ensureStarted(scope)
		startRecomposer()
	}

	private fun startRecomposer() {
		scope.launch(start = CoroutineStart.UNDISPATCHED) {
			recomposer.runRecomposeAndApplyChanges()
		}
	}

	fun sendFrames(): Job {
		return scope.launch {
			val ctrlC = KeyEvent("c", ctrl = true)

			while (true) {
				// Drain any pending key events before triggering the frame.
				while (true) {
					val keyEvent = keyEvents.tryReceive().getOrNull() ?: break
					val keyHandled = rootNode.sendKeyEvent(keyEvent)
					if (!keyHandled && keyEvent == ctrlC) {
						cancel()
					}
				}

				clock.sendFrame(0L) // Frame time value is not used by Compose runtime.
				delay(50L)
			}
		}
	}

	fun setContent(content: @Composable () -> Unit) {
		composition.setContent {
			CompositionLocalProvider(LocalTerminal provides terminalState.value) {
				content()
			}
		}
	}

	suspend fun awaitComplete() {
		try {
			val effectJob = checkNotNull(recomposer.effectCoroutineContext[Job]) {
				"No Job in effectCoroutineContext of recomposer"
			}
			effectJob.children.forEach { it.join() }
			recomposer.awaitIdle()

			recomposer.close()
			recomposer.join()
		} finally {
			job.cancel()
		}
	}

	fun cancel() {
		recomposer.cancel()
		job.cancel()
	}
}

internal class MosaicNodeApplier(
	private val onEndChanges: (MosaicNode) -> Unit = {},
) : AbstractApplier<MosaicNode>(
	root = MosaicNode(
		measurePolicy = BoxMeasurePolicy(),
		debugPolicy = { children.joinToString(separator = "\n") },
		onStaticDraw = null,
	),
) {
	override fun onEndChanges() {
		onEndChanges.invoke(root)
	}

	override fun insertTopDown(index: Int, instance: MosaicNode) {
		// Ignored, we insert bottom-up.
	}

	override fun insertBottomUp(index: Int, instance: MosaicNode) {
		current.children.add(index, instance)
	}

	override fun remove(index: Int, count: Int) {
		current.children.remove(index, count)
	}

	override fun move(from: Int, to: Int, count: Int) {
		current.children.move(from, to, count)
	}

	override fun onClear() {}
}

internal class GlobalSnapshotManager {
	private val started = AtomicBoolean(false)
	private val sent = AtomicBoolean(false)

	fun ensureStarted(scope: CoroutineScope) {
		if (started.compareAndSet(expect = false, update = true)) {
			val channel = Channel<Unit>(1)
			scope.launch {
				channel.consumeEach {
					sent.set(false)
					Snapshot.sendApplyNotifications()
				}
			}
			Snapshot.registerGlobalWriteObserver {
				if (sent.compareAndSet(expect = false, update = true)) {
					channel.trySend(Unit)
				}
			}
		}
	}
}
