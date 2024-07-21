import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    `java-library`
    kotlin("jvm") version "2.0.0"
    id("org.spongepowered.gradle.plugin") version "2.2.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

configurations.spongeRuntime {
    resolutionStrategy {
        eachDependency {
            if (target.name == "spongevanilla") {
                useVersion("1.20.6-11.0.0")
            }
        }
    }
}

sponge {
    apiVersion("11.0.0")
    license("MIT")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }
    plugin("smallerhungrier") {
        displayName("Smaller is Hungrier")
        entrypoint("io.github.pandier.smallerhungrier.SmallerHungrier")
        description("Have you never wanted to make players smaller when they are hungrier?")
        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
