plugins {
    id("dev.kikugie.stonecutter")
    alias(libs.plugins.fabric.loom) apply false
}
stonecutter active "1.20.2" /* [SC] DO NOT EDIT */

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    group = "project"
    ofTask("build")
}
