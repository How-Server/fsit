package dev.rvbsm.fsit.client.option

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import kotlin.time.Duration
import kotlin.time.TimeSource

@Environment(EnvType.CLIENT)
class HybridKeyBinding(
    id: String,
    code: Int,
    category: String,
    private val duration: Duration,
    private val modeGetter: () -> KeyBindingMode,
) : KeyBinding(id, InputUtil.Type.KEYSYM, code, category) {

    private val timeSource = TimeSource.Monotonic
    private var pressMark = timeSource.markNow()
    private var prevPressed = false
    private var isSticky = false

    override fun setPressed(pressed: Boolean) {
        isSticky = when (modeGetter()) {
            KeyBindingMode.Toggle -> true
            KeyBindingMode.Hold -> false
            KeyBindingMode.Hybrid -> isSticky.let {
                if (pressed) isSticky.also { pressMark = timeSource.markNow() }
                else if (prevPressed) timeSource.markNow() - pressMark <= duration else isSticky
            }
        }

        if (isSticky && pressed) {
            super.setPressed(!isPressed)
        } else if (!isSticky) {
            super.setPressed(pressed)
        }

        prevPressed = pressed
    }

    internal fun untoggle() {
        super.setPressed(false)
    }
}
