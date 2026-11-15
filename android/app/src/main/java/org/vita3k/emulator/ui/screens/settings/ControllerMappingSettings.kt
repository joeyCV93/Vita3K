package org.vita3k.emulator.ui.screens.settings

import android.content.Context
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import org.vita3k.emulator.ConnectedGamepad
import org.vita3k.emulator.ControllerFamily
import org.vita3k.emulator.R
import org.vita3k.emulator.data.EmulatorConfig
import org.vita3k.emulator.ui.theme.ApplyDialogDim
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SDL_GAMEPAD_BUTTON_SOUTH = 0
private const val SDL_GAMEPAD_BUTTON_EAST = 1
private const val SDL_GAMEPAD_BUTTON_WEST = 2
private const val SDL_GAMEPAD_BUTTON_NORTH = 3
private const val SDL_GAMEPAD_BUTTON_BACK = 4
private const val SDL_GAMEPAD_BUTTON_GUIDE = 5
private const val SDL_GAMEPAD_BUTTON_START = 6
private const val SDL_GAMEPAD_BUTTON_LEFT_STICK = 7
private const val SDL_GAMEPAD_BUTTON_RIGHT_STICK = 8
private const val SDL_GAMEPAD_BUTTON_LEFT_SHOULDER = 9
private const val SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER = 10
private const val SDL_GAMEPAD_BUTTON_DPAD_UP = 11
private const val SDL_GAMEPAD_BUTTON_DPAD_DOWN = 12
private const val SDL_GAMEPAD_BUTTON_DPAD_LEFT = 13
private const val SDL_GAMEPAD_BUTTON_DPAD_RIGHT = 14

private const val SDL_GAMEPAD_AXIS_LEFTX = 0
private const val SDL_GAMEPAD_AXIS_LEFTY = 1
private const val SDL_GAMEPAD_AXIS_RIGHTX = 2
private const val SDL_GAMEPAD_AXIS_RIGHTY = 3
private const val SDL_GAMEPAD_AXIS_LEFT_TRIGGER = 4
private const val SDL_GAMEPAD_AXIS_RIGHT_TRIGGER = 5

private data class ButtonBindingRow(
    @StringRes val labelRes: Int,
    val bindIndex: Int
)

private data class AxisBindingRow(
    @StringRes val labelRes: Int,
    val bindIndex: Int
)

private data class ManualBindingOption(
    val label: String,
    val value: Int
)

private enum class CaptureMode {
    Button,
    Axis
}

private val buttonBindingRows = listOf(
    ButtonBindingRow(R.string.settings_controls_bind_dpad_up, SDL_GAMEPAD_BUTTON_DPAD_UP),
    ButtonBindingRow(R.string.settings_controls_bind_dpad_down, SDL_GAMEPAD_BUTTON_DPAD_DOWN),
    ButtonBindingRow(R.string.settings_controls_bind_dpad_left, SDL_GAMEPAD_BUTTON_DPAD_LEFT),
    ButtonBindingRow(R.string.settings_controls_bind_dpad_right, SDL_GAMEPAD_BUTTON_DPAD_RIGHT),
    ButtonBindingRow(R.string.settings_controls_bind_triangle, SDL_GAMEPAD_BUTTON_NORTH),
    ButtonBindingRow(R.string.settings_controls_bind_circle, SDL_GAMEPAD_BUTTON_EAST),
    ButtonBindingRow(R.string.settings_controls_bind_cross, SDL_GAMEPAD_BUTTON_SOUTH),
    ButtonBindingRow(R.string.settings_controls_bind_square, SDL_GAMEPAD_BUTTON_WEST),
    ButtonBindingRow(R.string.settings_controls_bind_l1, SDL_GAMEPAD_BUTTON_LEFT_SHOULDER),
    ButtonBindingRow(R.string.settings_controls_bind_r1, SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER),
    ButtonBindingRow(R.string.settings_controls_bind_l3, SDL_GAMEPAD_BUTTON_LEFT_STICK),
    ButtonBindingRow(R.string.settings_controls_bind_r3, SDL_GAMEPAD_BUTTON_RIGHT_STICK),
    ButtonBindingRow(R.string.settings_controls_bind_select, SDL_GAMEPAD_BUTTON_BACK),
    ButtonBindingRow(R.string.settings_controls_bind_start, SDL_GAMEPAD_BUTTON_START),
    ButtonBindingRow(R.string.settings_controls_bind_ps_button, SDL_GAMEPAD_BUTTON_GUIDE)
)

private val axisBindingRows = listOf(
    AxisBindingRow(R.string.settings_controls_bind_left_stick_up, SDL_GAMEPAD_AXIS_LEFTY),
    AxisBindingRow(R.string.settings_controls_bind_left_stick_down, SDL_GAMEPAD_AXIS_LEFTY),
    AxisBindingRow(R.string.settings_controls_bind_left_stick_left, SDL_GAMEPAD_AXIS_LEFTX),
    AxisBindingRow(R.string.settings_controls_bind_left_stick_right, SDL_GAMEPAD_AXIS_LEFTX),
    AxisBindingRow(R.string.settings_controls_bind_right_stick_up, SDL_GAMEPAD_AXIS_RIGHTY),
    AxisBindingRow(R.string.settings_controls_bind_right_stick_down, SDL_GAMEPAD_AXIS_RIGHTY),
    AxisBindingRow(R.string.settings_controls_bind_right_stick_left, SDL_GAMEPAD_AXIS_RIGHTX),
    AxisBindingRow(R.string.settings_controls_bind_right_stick_right, SDL_GAMEPAD_AXIS_RIGHTX),
    AxisBindingRow(R.string.settings_controls_bind_l2, SDL_GAMEPAD_AXIS_LEFT_TRIGGER),
    AxisBindingRow(R.string.settings_controls_bind_r2, SDL_GAMEPAD_AXIS_RIGHT_TRIGGER)
)

@Composable
internal fun ControllerMappingSections(
    cfg: EmulatorConfig,
    connectedGamepads: List<ConnectedGamepad>,
    onUpdate: (EmulatorConfig.() -> Unit) -> Unit,
    onShowHelp: (SettingsHelpEntry) -> Unit
) {
    val controllerFamily = connectedGamepads.firstOrNull()?.family ?: ControllerFamily.Standard
    var pendingButtonBinding by remember { mutableStateOf<ButtonBindingRow?>(null) }
    var pendingAxisBinding by remember { mutableStateOf<AxisBindingRow?>(null) }
    var pendingChordTarget by remember { mutableStateOf<ChordTarget?>(null) }

    SettingsSectionCard(
        title = stringResource(R.string.settings_controls_connected_title),
        summary = if (connectedGamepads.isEmpty()) {
            stringResource(R.string.settings_controls_connected_none)
        } else {
            connectedGamepads.joinToString(separator = "\n") { it.name }
        },
        help = SettingsHelpEntry(
            title = stringResource(R.string.settings_controls_connected_title),
            body = stringResource(R.string.settings_controls_connected_desc),
            scope = SettingsScope.Global
        ),
        onShowHelp = onShowHelp
    ) {
        if (connectedGamepads.isEmpty()) {
            SettingsNote(text = stringResource(R.string.settings_controls_manual_available))
        }
    }

    SettingsSectionCard(
        title = stringResource(R.string.settings_controls_mapping_buttons_title),
        summary = stringResource(R.string.settings_controls_mapping_buttons_desc),
        help = SettingsHelpEntry(
            title = stringResource(R.string.settings_controls_mapping_buttons_title),
            body = stringResource(R.string.settings_controls_mapping_buttons_help),
            scope = SettingsScope.Global
        ),
        onShowHelp = onShowHelp
    ) {
        buttonBindingRows.forEach { row ->
            SettingsActionRow(
                title = stringResource(row.labelRes),
                value = buttonLabel(controllerFamily, currentButtonBinding(cfg, row.bindIndex)),
                onClick = { pendingButtonBinding = row },
                help = null,
                onShowHelp = onShowHelp
            )
        }
    }

    SettingsSectionCard(
        title = stringResource(R.string.settings_controls_mapping_axes_title),
        summary = stringResource(R.string.settings_controls_mapping_axes_desc),
        help = SettingsHelpEntry(
            title = stringResource(R.string.settings_controls_mapping_axes_title),
            body = stringResource(R.string.settings_controls_mapping_axes_help),
            scope = SettingsScope.Global
        ),
        onShowHelp = onShowHelp
    ) {
        SettingsNote(text = stringResource(R.string.settings_controls_mapping_axes_note))
        axisBindingRows.forEach { row ->
            SettingsActionRow(
                title = stringResource(row.labelRes),
                value = axisLabel(controllerFamily, currentAxisBinding(cfg, row.bindIndex)),
                onClick = { pendingAxisBinding = row },
                help = null,
                onShowHelp = onShowHelp
            )
        }
    }

    // Custom hotkeys: press a chord (buttons held together) from a controller.
    SettingsSectionCard(
        title = "Controller hotkeys",
        summary = "Hold a combination of buttons to trigger an action without touching the screen.",
        help = null,
        onShowHelp = onShowHelp
    ) {
        SettingsActionRow(
            title = "Open menu",
            value = chordLabel(controllerFamily, cfg.menuHotkeyChord),
            onClick = { pendingChordTarget = ChordTarget.Menu },
            help = null,
            onShowHelp = onShowHelp
        )
        SettingsActionRow(
            title = "Game selection screen",
            value = chordLabel(controllerFamily, cfg.libraryHotkeyChord),
            onClick = { pendingChordTarget = ChordTarget.Library },
            help = null,
            onShowHelp = onShowHelp
        )
        SettingsActionRow(
            title = "Quit Vita3K completely",
            value = chordLabel(controllerFamily, cfg.exitHotkeyChord),
            onClick = { pendingChordTarget = ChordTarget.Exit },
            help = null,
            onShowHelp = onShowHelp
        )
    }

    SettingsSectionCard(
        title = stringResource(R.string.settings_controls_behavior_title),
        summary = null,
        help = null,
        onShowHelp = onShowHelp
    ) {
        SettingsSliderRow(
            title = stringResource(R.string.settings_controls_analog_multiplier),
            valueLabel = stringResource(
                R.string.settings_controls_multiplier_value,
                cfg.controllerAnalogMultiplier.coerceIn(0.1f, 2.0f)
            ),
            value = (cfg.controllerAnalogMultiplier * 100f).coerceIn(10f, 200f),
            onValueChange = {
                onUpdate {
                    controllerAnalogMultiplier = (it.roundToInt() / 100f).coerceIn(0.1f, 2.0f)
                }
            },
            valueRange = 10f..200f,
            steps = 189,
            help = SettingsHelpEntry(
                title = stringResource(R.string.settings_controls_analog_multiplier),
                body = stringResource(R.string.settings_controls_analog_multiplier_desc),
                scope = SettingsScope.Global
            ),
            onShowHelp = onShowHelp
        )
        SettingsToggleRow(
            title = stringResource(R.string.settings_controls_disable_motion),
            checked = cfg.disableMotion,
            onCheckedChange = { checked ->
                onUpdate { disableMotion = checked }
            },
            help = SettingsHelpEntry(
                title = stringResource(R.string.settings_controls_disable_motion),
                body = stringResource(R.string.settings_controls_disable_motion_desc),
                scope = SettingsScope.Global
            ),
            onShowHelp = onShowHelp
        )
    }

    pendingButtonBinding?.let { row ->
        BindingCaptureDialog(
            title = stringResource(row.labelRes),
            prompt = stringResource(R.string.settings_controls_capture_button_prompt),
            options = buttonManualOptions(controllerFamily),
            selectedValue = currentButtonBinding(cfg, row.bindIndex),
            captureMode = CaptureMode.Button,
            connectedGamepads = connectedGamepads,
            onDismiss = { pendingButtonBinding = null },
            onSelected = { mappedButton ->
                onUpdate {
                    controllerBinds = controllerBinds.copyOf().also { binds ->
                        binds[row.bindIndex] = mappedButton
                    }
                }
                pendingButtonBinding = null
            }
        )
    }

    pendingChordTarget?.let { target ->
        ChordCaptureDialog(
            title = when (target) {
                ChordTarget.Menu -> "Open menu"
                ChordTarget.Library -> "Game selection screen"
                ChordTarget.Exit -> "Quit Vita3K completely"
            },
            family = controllerFamily,
            connected = connectedGamepads.isNotEmpty(),
            onDismiss = { pendingChordTarget = null },
            onChord = { chord ->
                onUpdate {
                    when (target) {
                        ChordTarget.Menu -> menuHotkeyChord = chord
                        ChordTarget.Library -> libraryHotkeyChord = chord
                        ChordTarget.Exit -> exitHotkeyChord = chord
                    }
                }
                pendingChordTarget = null
            }
        )
    }

    pendingAxisBinding?.let { row ->
        BindingCaptureDialog(
            title = stringResource(row.labelRes),
            prompt = stringResource(R.string.settings_controls_capture_axis_prompt),
            options = axisManualOptions(controllerFamily),
            selectedValue = currentAxisBinding(cfg, row.bindIndex),
            captureMode = CaptureMode.Axis,
            connectedGamepads = connectedGamepads,
            onDismiss = { pendingAxisBinding = null },
            onSelected = { mappedAxis ->
                onUpdate {
                    controllerAxisBinds = controllerAxisBinds.copyOf().also { binds ->
                        binds[row.bindIndex] = mappedAxis
                    }
                }
                pendingAxisBinding = null
            }
        )
    }
}

@Composable
private fun BindingCaptureDialog(
    title: String,
    prompt: String,
    options: List<ManualBindingOption>,
    selectedValue: Int,
    captureMode: CaptureMode,
    connectedGamepads: List<ConnectedGamepad>,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit
) {
    // Capture only starts once Record is chosen, so the controller can also reach Cancel.
    var recording by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(0) } // 0 Record, 1 Cancel
    val activate: () -> Unit = {
        if (selected == 0) recording = true else onDismiss()
    }
    androidx.compose.runtime.LaunchedEffect(recording) {
        if (recording) {
            kotlinx.coroutines.delay(8000)
            recording = false
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        ApplyDialogDim()
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (connectedGamepads.isNotEmpty()) {
                    Text(
                        text = connectedGamepads.joinToString(separator = "\n") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CaptureInputSurface(
                    captureMode = captureMode,
                    enabled = connectedGamepads.isNotEmpty(),
                    recording = recording,
                    onCaptured = onSelected,
                    onNav = { delta -> selected = (selected + delta).coerceIn(0, 1) },
                    onActivate = activate,
                    onBack = onDismiss
                )
                // Record / Cancel sit right under the prompt so they stay on screen on short (landscape) displays.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    val ring = MaterialTheme.colorScheme.primary
                    fun ringFor(index: Int) = if (selected == index && !recording) {
                        Modifier
                            .background(ring.copy(alpha = 0.30f), CircleShape)
                            .border(4.dp, ring, CircleShape)
                    } else Modifier
                    TextButton(
                        onClick = { selected = 0; activate() },
                        modifier = ringFor(0)
                    ) { Text(if (recording) "Recording..." else "Record") }
                    TextButton(
                        onClick = onDismiss,
                        modifier = ringFor(1)
                    ) { Text(stringResource(R.string.action_cancel)) }
                }
                SettingsScrollableChoiceSelector(
                    title = stringResource(R.string.settings_controls_manual_selection),
                    options = options.map { it.label },
                    selectedIndex = options.indexOfFirst { it.value == selectedValue }.coerceAtLeast(0),
                    onSelected = { index -> onSelected(options[index].value) },
                    summary = options.firstOrNull { it.value == selectedValue }?.label,
                    help = null,
                    onShowHelp = {}
                )
            }
        }
    }
}

@Composable
private fun CaptureInputSurface(
    captureMode: CaptureMode,
    enabled: Boolean,
    recording: Boolean,
    onCaptured: (Int) -> Unit,
    onNav: (Int) -> Unit,
    onActivate: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (enabled && !recording) {
                    "Press Record, then the button or stick to assign. D-pad left/right and A choose below, B cancels."
                } else if (enabled) {
                    stringResource(R.string.settings_controls_capture_listening)
                } else {
                    stringResource(R.string.settings_controls_capture_waiting_for_controller)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            AndroidView(
                factory = { context ->
                    GamepadCaptureView(context)
                },
                update = { view ->
                    view.captureMode = captureMode
                    view.recording = recording
                    view.onCaptured = onCaptured
                    view.onNav = onNav
                    view.onActivate = onActivate
                    view.onBack = onBack
                    if (enabled) {
                        view.post {
                            view.requestFocusFromTouch()
                            view.requestFocus()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
        }
    }
}

private class GamepadCaptureView(context: Context) : View(context) {
    var captureMode: CaptureMode = CaptureMode.Button
    var onCaptured: (Int) -> Unit = {}

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        org.vita3k.emulator.Emulator.controllerCaptureActive = true
        post {
            requestFocusFromTouch()
            requestFocus()
        }
    }

    override fun onDetachedFromWindow() {
        org.vita3k.emulator.Emulator.controllerCaptureActive = false
        super.onDetachedFromWindow()
    }

    var recording = false
    var onNav: (Int) -> Unit = {}
    var onActivate: () -> Unit = {}
    var onBack: () -> Unit = {}
    private var hatX = 0

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!recording) {
            if (!isGamepadDevice(event.device)) return super.dispatchKeyEvent(event)
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP -> onNav(-1)
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN -> onNav(1)
                    KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER -> onActivate()
                    KeyEvent.KEYCODE_BUTTON_B -> onBack()
                }
            }
            return true
        }
        if (captureMode != CaptureMode.Button) {
            return super.dispatchKeyEvent(event)
        }
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0 || !isGamepadDevice(event.device)) {
            return super.dispatchKeyEvent(event)
        }

        val mapped = keyCodeToSdlButton(event.keyCode) ?: return super.dispatchKeyEvent(event)
        onCaptured(mapped)
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (!recording) {
            if (!event.isFromSource(InputDevice.SOURCE_JOYSTICK)) return super.onGenericMotionEvent(event)
            val x = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val dir = if (x < -0.5f) -1 else if (x > 0.5f) 1 else 0
            if (dir != hatX) {
                hatX = dir
                if (dir != 0) onNav(dir)
            }
            return true
        }
        if (captureMode != CaptureMode.Axis || !event.isFromSource(InputDevice.SOURCE_JOYSTICK)) {
            return super.onGenericMotionEvent(event)
        }

        val mapped = dominantAxisMapping(event) ?: return super.onGenericMotionEvent(event)
        onCaptured(mapped)
        return true
    }
}

private fun isGamepadDevice(device: InputDevice?): Boolean {
    device ?: return false
    val sources = device.sources
    return (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
        (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
}

private fun dominantAxisMapping(event: MotionEvent): Int? {
    val candidates = listOf(
        MotionEvent.AXIS_X to SDL_GAMEPAD_AXIS_LEFTX,
        MotionEvent.AXIS_Y to SDL_GAMEPAD_AXIS_LEFTY,
        MotionEvent.AXIS_Z to SDL_GAMEPAD_AXIS_RIGHTX,
        MotionEvent.AXIS_RZ to SDL_GAMEPAD_AXIS_RIGHTY,
        MotionEvent.AXIS_RX to SDL_GAMEPAD_AXIS_RIGHTX,
        MotionEvent.AXIS_RY to SDL_GAMEPAD_AXIS_RIGHTY,
        MotionEvent.AXIS_LTRIGGER to SDL_GAMEPAD_AXIS_LEFT_TRIGGER,
        MotionEvent.AXIS_BRAKE to SDL_GAMEPAD_AXIS_LEFT_TRIGGER,
        MotionEvent.AXIS_RTRIGGER to SDL_GAMEPAD_AXIS_RIGHT_TRIGGER,
        MotionEvent.AXIS_GAS to SDL_GAMEPAD_AXIS_RIGHT_TRIGGER
    )

    var bestValue = 0f
    var bestAxis: Int? = null
    for ((androidAxis, sdlAxis) in candidates) {
        val value = abs(event.getAxisValue(androidAxis))
        if (value > 0.6f && value > bestValue) {
            bestValue = value
            bestAxis = sdlAxis
        }
    }
    return bestAxis
}

private fun keyCodeToSdlButton(keyCode: Int): Int? {
    return when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> SDL_GAMEPAD_BUTTON_SOUTH
        KeyEvent.KEYCODE_BUTTON_B -> SDL_GAMEPAD_BUTTON_EAST
        KeyEvent.KEYCODE_BUTTON_X -> SDL_GAMEPAD_BUTTON_WEST
        KeyEvent.KEYCODE_BUTTON_Y -> SDL_GAMEPAD_BUTTON_NORTH
        KeyEvent.KEYCODE_BUTTON_SELECT -> SDL_GAMEPAD_BUTTON_BACK
        KeyEvent.KEYCODE_BUTTON_MODE -> SDL_GAMEPAD_BUTTON_GUIDE
        KeyEvent.KEYCODE_BUTTON_START -> SDL_GAMEPAD_BUTTON_START
        KeyEvent.KEYCODE_BUTTON_THUMBL -> SDL_GAMEPAD_BUTTON_LEFT_STICK
        KeyEvent.KEYCODE_BUTTON_THUMBR -> SDL_GAMEPAD_BUTTON_RIGHT_STICK
        KeyEvent.KEYCODE_BUTTON_L1 -> SDL_GAMEPAD_BUTTON_LEFT_SHOULDER
        KeyEvent.KEYCODE_BUTTON_R1 -> SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER
        KeyEvent.KEYCODE_DPAD_UP -> SDL_GAMEPAD_BUTTON_DPAD_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> SDL_GAMEPAD_BUTTON_DPAD_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> SDL_GAMEPAD_BUTTON_DPAD_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> SDL_GAMEPAD_BUTTON_DPAD_RIGHT
        else -> null
    }
}

private fun currentButtonBinding(cfg: EmulatorConfig, index: Int): Int {
    return if (index in cfg.controllerBinds.indices) {
        cfg.controllerBinds[index]
    } else {
        index
    }
}

private fun currentAxisBinding(cfg: EmulatorConfig, index: Int): Int {
    return if (index in cfg.controllerAxisBinds.indices) {
        cfg.controllerAxisBinds[index]
    } else {
        index
    }
}

private fun buttonManualOptions(family: ControllerFamily): List<ManualBindingOption> = listOf(
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_DPAD_UP), SDL_GAMEPAD_BUTTON_DPAD_UP),
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_DPAD_DOWN), SDL_GAMEPAD_BUTTON_DPAD_DOWN),
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_DPAD_LEFT), SDL_GAMEPAD_BUTTON_DPAD_LEFT),
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_DPAD_RIGHT), SDL_GAMEPAD_BUTTON_DPAD_RIGHT),
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_LEFT_SHOULDER), SDL_GAMEPAD_BUTTON_LEFT_SHOULDER),
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER), SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER),
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_LEFT_STICK), SDL_GAMEPAD_BUTTON_LEFT_STICK),
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_RIGHT_STICK), SDL_GAMEPAD_BUTTON_RIGHT_STICK),
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_BACK), SDL_GAMEPAD_BUTTON_BACK),
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_GUIDE), SDL_GAMEPAD_BUTTON_GUIDE),
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_START), SDL_GAMEPAD_BUTTON_START),
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_NORTH), SDL_GAMEPAD_BUTTON_NORTH),
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_EAST), SDL_GAMEPAD_BUTTON_EAST),
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_SOUTH), SDL_GAMEPAD_BUTTON_SOUTH),
    ManualBindingOption(buttonLabel(family, SDL_GAMEPAD_BUTTON_WEST), SDL_GAMEPAD_BUTTON_WEST)
)

private fun axisManualOptions(family: ControllerFamily): List<ManualBindingOption> = listOf(
    ManualBindingOption(axisLabel(family, SDL_GAMEPAD_AXIS_LEFTX), SDL_GAMEPAD_AXIS_LEFTX),
    ManualBindingOption(axisLabel(family, SDL_GAMEPAD_AXIS_LEFTY), SDL_GAMEPAD_AXIS_LEFTY),
    ManualBindingOption(axisLabel(family, SDL_GAMEPAD_AXIS_RIGHTX), SDL_GAMEPAD_AXIS_RIGHTX),
    ManualBindingOption(axisLabel(family, SDL_GAMEPAD_AXIS_RIGHTY), SDL_GAMEPAD_AXIS_RIGHTY),
    ManualBindingOption(axisLabel(family, SDL_GAMEPAD_AXIS_LEFT_TRIGGER), SDL_GAMEPAD_AXIS_LEFT_TRIGGER),
    ManualBindingOption(axisLabel(family, SDL_GAMEPAD_AXIS_RIGHT_TRIGGER), SDL_GAMEPAD_AXIS_RIGHT_TRIGGER)
)

private fun buttonLabel(family: ControllerFamily, button: Int): String {
    return when (button) {
        SDL_GAMEPAD_BUTTON_DPAD_UP -> "D-Pad Up"
        SDL_GAMEPAD_BUTTON_DPAD_DOWN -> "D-Pad Down"
        SDL_GAMEPAD_BUTTON_DPAD_LEFT -> "D-Pad Left"
        SDL_GAMEPAD_BUTTON_DPAD_RIGHT -> "D-Pad Right"
        SDL_GAMEPAD_BUTTON_BACK -> when (family) {
            ControllerFamily.Xbox -> "Back"
            ControllerFamily.Nintendo -> "-"
            ControllerFamily.Standard -> "Select"
        }
        SDL_GAMEPAD_BUTTON_GUIDE -> when (family) {
            ControllerFamily.Xbox -> "Guide"
            ControllerFamily.Nintendo -> "Home"
            ControllerFamily.Standard -> "PS"
        }
        SDL_GAMEPAD_BUTTON_START -> when (family) {
            ControllerFamily.Nintendo -> "+"
            else -> "Start"
        }
        SDL_GAMEPAD_BUTTON_LEFT_STICK -> if (family == ControllerFamily.Standard) "L3" else "LS"
        SDL_GAMEPAD_BUTTON_RIGHT_STICK -> if (family == ControllerFamily.Standard) "R3" else "RS"
        SDL_GAMEPAD_BUTTON_LEFT_SHOULDER -> when (family) {
            ControllerFamily.Xbox -> "LB"
            ControllerFamily.Nintendo -> "L"
            ControllerFamily.Standard -> "L1"
        }
        SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER -> when (family) {
            ControllerFamily.Xbox -> "RB"
            ControllerFamily.Nintendo -> "R"
            ControllerFamily.Standard -> "R1"
        }
        SDL_GAMEPAD_BUTTON_SOUTH -> when (family) {
            ControllerFamily.Xbox -> "A"
            ControllerFamily.Nintendo -> "B"
            ControllerFamily.Standard -> "Cross"
        }
        SDL_GAMEPAD_BUTTON_EAST -> when (family) {
            ControllerFamily.Xbox -> "B"
            ControllerFamily.Nintendo -> "A"
            ControllerFamily.Standard -> "Circle"
        }
        SDL_GAMEPAD_BUTTON_WEST -> when (family) {
            ControllerFamily.Xbox -> "X"
            ControllerFamily.Nintendo -> "Y"
            ControllerFamily.Standard -> "Square"
        }
        SDL_GAMEPAD_BUTTON_NORTH -> when (family) {
            ControllerFamily.Xbox -> "Y"
            ControllerFamily.Nintendo -> "X"
            ControllerFamily.Standard -> "Triangle"
        }
        else -> "Button $button"
    }
}

private fun axisLabel(family: ControllerFamily, axis: Int): String {
    return when (axis) {
        SDL_GAMEPAD_AXIS_LEFTX -> "Left X"
        SDL_GAMEPAD_AXIS_LEFTY -> "Left Y"
        SDL_GAMEPAD_AXIS_RIGHTX -> "Right X"
        SDL_GAMEPAD_AXIS_RIGHTY -> "Right Y"
        SDL_GAMEPAD_AXIS_LEFT_TRIGGER -> when (family) {
            ControllerFamily.Xbox -> "LT"
            ControllerFamily.Nintendo -> "ZL"
            ControllerFamily.Standard -> "L2"
        }
        SDL_GAMEPAD_AXIS_RIGHT_TRIGGER -> when (family) {
            ControllerFamily.Xbox -> "RT"
            ControllerFamily.Nintendo -> "ZR"
            ControllerFamily.Standard -> "R2"
        }
        else -> "Axis $axis"
    }
}

// ---- Custom controller hotkey chords -------------------------------------------------------------
// A chord is a bitmask: bit N = SDL gamepad button N, bit 28 = L2, bit 29 = R2. It must match
// ctrl_get_held_chord() on the native side. 0 means "not set".

private enum class ChordTarget {
    Menu,
    Library,
    Exit
}

private const val CHORD_LEFT_TRIGGER_BIT = 1 shl 28
private const val CHORD_RIGHT_TRIGGER_BIT = 1 shl 29
private const val MIN_CHORD_BUTTONS = 2

@Composable
private fun ChordCaptureDialog(
    title: String,
    family: ControllerFamily,
    connected: Boolean,
    onDismiss: () -> Unit,
    onChord: (Int) -> Unit
) {
    var preview by remember { mutableStateOf(0) }
    // Recording only starts when "Record" is chosen, so the controller can also reach Clear and Cancel.
    var recording by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(0) } // 0 Record, 1 Clear, 2 Cancel
    val activate: () -> Unit = {
        when (selected) {
            0 -> {
                preview = 0
                recording = true
            }
            1 -> onChord(0)
            else -> onDismiss()
        }
    }
    // Give up on a recording that has seen no new button for a while, so the dialog never traps the controller.
    androidx.compose.runtime.LaunchedEffect(recording, preview) {
        if (recording) {
            kotlinx.coroutines.delay(8000)
            recording = false
            preview = 0
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        ApplyDialogDim()
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choose Record, then hold the buttons you want to use together and let go. Use at least $MIN_CHORD_BUTTONS buttons so it can't trigger by accident. D-pad left/right and A choose a button below, B cancels.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = when {
                                !connected -> "Connect a controller to record a chord."
                                !recording -> "Press Record to set a new chord."
                                preview == 0 -> "Listening for buttons... (gives up after a few seconds)"
                                else -> chordLabel(family, preview)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AndroidView(
                            factory = { context ->
                                ChordCaptureView(context)
                            },
                            update = { view ->
                                view.recording = recording
                                view.onPreview = { preview = it }
                                view.onChord = onChord
                                view.onNav = { delta -> selected = (selected + delta).coerceIn(0, 2) }
                                view.onActivate = activate
                                view.onBack = onDismiss
                                if (connected) {
                                    view.post {
                                        view.requestFocusFromTouch()
                                        view.requestFocus()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    val ring = MaterialTheme.colorScheme.primary
                    fun ringFor(index: Int) = if (selected == index && !recording) {
                        Modifier
                            .background(ring.copy(alpha = 0.30f), CircleShape)
                            .border(4.dp, ring, CircleShape)
                    } else Modifier
                    TextButton(
                        onClick = { selected = 0; activate() },
                        modifier = ringFor(0)
                    ) { Text(if (recording) "Recording..." else "Record") }
                    TextButton(
                        onClick = { onChord(0) },
                        modifier = ringFor(1)
                    ) { Text("Clear") }
                    TextButton(
                        onClick = onDismiss,
                        modifier = ringFor(2)
                    ) { Text(stringResource(R.string.action_cancel)) }
                }
            }
        }
    }
}

private class ChordCaptureView(context: Context) : View(context) {
    var onPreview: (Int) -> Unit = {}
    var onChord: (Int) -> Unit = {}

    private var held = 0
    private var peak = 0

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Stops the emulator treating B / Back as "close the menu" while a button is being recorded.
        org.vita3k.emulator.Emulator.controllerCaptureActive = true
        post {
            requestFocusFromTouch()
            requestFocus()
        }
    }

    override fun onDetachedFromWindow() {
        org.vita3k.emulator.Emulator.controllerCaptureActive = false
        super.onDetachedFromWindow()
    }

    // When not recording the view only steers the dialog's three buttons.
    var recording = false
        set(value) {
            if (field != value) {
                held = 0
                peak = 0
            }
            field = value
        }
    var onNav: (Int) -> Unit = {}
    var onActivate: () -> Unit = {}
    var onBack: () -> Unit = {}
    private var hatX = 0

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!isGamepadDevice(event.device)) {
            return super.dispatchKeyEvent(event)
        }
        if (!recording) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP -> onNav(-1)
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN -> onNav(1)
                    KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER -> onActivate()
                    KeyEvent.KEYCODE_BUTTON_B -> onBack()
                }
            }
            return true
        }
        val bit = keyCodeToChordBit(event.keyCode) ?: return super.dispatchKeyEvent(event)
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) press(bit)
            KeyEvent.ACTION_UP -> release(bit)
        }
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (!event.isFromSource(InputDevice.SOURCE_JOYSTICK)) {
            return super.onGenericMotionEvent(event)
        }
        if (!recording) {
            val x = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val dir = if (x < -0.5f) -1 else if (x > 0.5f) 1 else 0
            if (dir != hatX) {
                hatX = dir
                if (dir != 0) onNav(dir)
            }
            return true
        }
        val left = maxOf(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE))
        val right = maxOf(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS))
        updateTrigger(CHORD_LEFT_TRIGGER_BIT, left)
        updateTrigger(CHORD_RIGHT_TRIGGER_BIT, right)
        return true
    }

    private fun updateTrigger(bit: Int, value: Float) {
        val isHeld = held and bit != 0
        if (!isHeld && value > 0.6f) press(bit) else if (isHeld && value < 0.3f) release(bit)
    }

    private fun press(bit: Int) {
        held = held or bit
        if (Integer.bitCount(held) >= Integer.bitCount(peak)) {
            peak = held
            onPreview(peak)
        }
    }

    private fun release(bit: Int) {
        held = held and bit.inv()
        if (held != 0) return
        if (Integer.bitCount(peak) >= MIN_CHORD_BUTTONS) {
            onChord(peak)
        } else {
            peak = 0
            onPreview(0)
        }
    }
}

private fun keyCodeToChordBit(keyCode: Int): Int? {
    keyCodeToSdlButton(keyCode)?.let { return 1 shl it }
    return when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_L2 -> CHORD_LEFT_TRIGGER_BIT
        KeyEvent.KEYCODE_BUTTON_R2 -> CHORD_RIGHT_TRIGGER_BIT
        else -> null
    }
}

private fun chordLabel(family: ControllerFamily, chord: Int): String {
    if (chord == 0) return "Not set"
    val parts = mutableListOf<String>()
    for (button in 0..27) {
        if (chord and (1 shl button) != 0) parts += buttonLabel(family, button)
    }
    if (chord and CHORD_LEFT_TRIGGER_BIT != 0) parts += axisLabel(family, SDL_GAMEPAD_AXIS_LEFT_TRIGGER)
    if (chord and CHORD_RIGHT_TRIGGER_BIT != 0) parts += axisLabel(family, SDL_GAMEPAD_AXIS_RIGHT_TRIGGER)
    return parts.joinToString(" + ")
}
