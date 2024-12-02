package dev.rvbsm.fsit.config

import dev.rvbsm.fsit.serialization.migration.migrateTo
import dev.rvbsm.fsit.serialization.migration.migrationKey
import dev.rvbsm.fsit.serialization.migration.version
import dev.rvbsm.fsit.util.text.splitOnce

internal val configSchemas = setOf(
    setOf(
        "sneak.enabled" migrateTo "sitting.on_double_sneak.enabled",
        "sneak.min_angle" migrateTo "sitting.on_double_sneak.min_pitch",
        "sneak.delay" migrateTo "sitting.on_double_sneak.delay",

        "misc.ride_players" migrateTo "riding.on_use.enabled",

        "misc.riding.enabled" migrateTo "riding.on_use.enabled",

        "sneak.angle" migrateTo "sitting.on_double_sneak.min_pitch",

        setOf(
            "sittable.blocks".migrationKey(),
            "sittable.tags".migrationKey { "#$it" },
        ) migrateTo "sitting.on_use.blocks",
    ) version 0, // mod v1 with "config_version"

    setOf(
        "sittable.enabled" migrateTo "on_use.sitting",
        "sittable.radius" migrateTo "on_use.range",
        "sittable.materials" migrateTo "on_use.blocks",
        "riding.enabled" migrateTo "on_use.riding",

        "sitting.seats_gravity" migrateTo "sitting.apply_gravity",
        "sitting.on_use.enabled" migrateTo "on_use.sitting",
        "sitting.on_use.range" migrateTo "on_use.range",
        "sitting.on_use.blocks" migrateTo "on_use.blocks",
        "sitting.on_double_sneak.enabled" migrateTo "on_double_sneak.sitting",
        "sitting.on_double_sneak.min_pitch" migrateTo "on_double_sneak.min_pitch",
        "sitting.on_double_sneak.delay" migrateTo "on_double_sneak.delay",
        "riding.on_use.enabled" migrateTo "on_use.riding",
    ) version 1, // mod v2 without "version"

    setOf(
        setOf(
            "sitting.apply_gravity".migrationKey { "$it+" },
            "sitting.allow_in_air".migrationKey(),
        ) migrateTo "sitting.behaviour".migrationKey {
            val (gravity, discard) = it.splitOnce('+').let { (f, s) -> f.toBoolean() to !s.toBoolean() }

            if (!gravity) {
                if (!discard) "nothing"
                else "discard"
            } else "gravity"
        },

        "on_double_sneak.sitting" migrateTo "on_sneak.sitting",
        "on_double_sneak.crawling" migrateTo "on_sneak.crawling",
        "on_double_sneak.min_pitch" migrateTo "on_sneak.min_pitch",
        "on_double_sneak.delay" migrateTo "on_sneak.delay",
    ) version 2,
)
