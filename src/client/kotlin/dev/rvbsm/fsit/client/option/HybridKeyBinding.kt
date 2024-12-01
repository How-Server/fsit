package dev.rvbsm.fsit.client.option

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.util.Util
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Environment(EnvType.CLIENT)
class HybridKeyBinding(
    id: String,
    code: Int,
    category: String,
    private val duration: Duration,
    private val modeGetter: () -> KeyBindingMode,
) : KeyBinding(id, InputUtil.Type.KEYSYM, code, category) {

    private var pressTime = 0L
    private var prevPressed = false
    private var isSticky = false

    override fun setPressed(pressed: Boolean) {
        isSticky = when (modeGetter()) {
            KeyBindingMode.Toggle -> true
            KeyBindingMode.Hold -> false
            KeyBindingMode.Hybrid -> isSticky.let {
                if (pressed) {
                    pressTime = Util.getMeasuringTimeMs()
                }

                if (pressed || !prevPressed) isSticky
                else (Util.getMeasuringTimeMs() - pressTime).milliseconds <= duration
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
