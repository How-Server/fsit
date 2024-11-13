package dev.rvbsm.fsit.client.option

import com.mojang.serialization.Codec
import net.minecraft.client.option.SimpleOption
import net.minecraft.util.TranslatableOption

inline fun <reified T> enumOption(
    key: String,
    defaultValue: T,
    tooltipFactory: SimpleOption.TooltipFactory<T> = SimpleOption.emptyTooltip(),
    valueTextGetter: SimpleOption.ValueTextGetter<T> = SimpleOption.enumValueText(),
    noinline changeCallback: (T) -> Unit = {},
) where T : Enum<T>, T : TranslatableOption = SimpleOption(
    key,
    tooltipFactory,
    valueTextGetter,
    SimpleOption.PotentialValuesBasedCallbacks(
        enumValues<T>().asList(),
        Codec.INT.xmap({ enumValues<T>().let { entries -> entries[it % entries.size] } }, Enum<T>::ordinal),
    ),
    defaultValue,
    changeCallback,
)
