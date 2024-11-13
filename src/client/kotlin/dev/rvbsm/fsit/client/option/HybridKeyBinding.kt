package dev.rvbsm.fsit.client.option

import dev.rvbsm.fsit.modTimeSource
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

    private var pressMark = modTimeSource.markNow()
    private var prevPressed = false
    private var isSticky = false

    override fun setPressed(pressed: Boolean) {
        isSticky = when (modeGetter()) {
            KeyBindingMode.Toggle -> true
            KeyBindingMode.Hold -> false
            KeyBindingMode.Hybrid -> isSticky.let {
                if (pressed) isSticky.also { pressMark = modTimeSource.markNow() }
                else if (prevPressed) pressMark.elapsedNow() <= duration else isSticky
            }
        }

        if (isSticky && pressed) {
            super.setPressed(!isPressed)
        } else if (!isSticky) {
            super.setPressed(pressed)
        }

        prevPressed = pressed
    }

    @PublishedApi
    internal fun untoggle() {
        super.setPressed(false)
    }
}
