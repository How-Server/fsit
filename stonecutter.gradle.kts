buildscript {
    dependencies.classpath("com.guardsquare:proguard-gradle:7.5.0")
}

plugins {
    id("dev.kikugie.stonecutter")

    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    alias(libs.plugins.fabric.loom) apply false
    alias(libs.plugins.publish) apply false
    alias(libs.plugins.shadow) apply false
}

stonecutter active "1.20" /* [SC] DO NOT EDIT */

tasks {
    stonecutter registerChiseled register("chiseledBuild", stonecutter.chiseled) {
        group = "project"
        ofTask("build")
    }

    stonecutter registerChiseled register("chiseledPublish", stonecutter.chiseled) {
        group = "project"
        ofTask("publishMods")
    }
}

val gitVersion: String by extra {
    providers.exec {
        executable = "git"
        args = listOf("describe", "--tags", "--dirty", "--always")
    }.standardOutput.asText.map { it.trim().drop(1) }.get()
}
