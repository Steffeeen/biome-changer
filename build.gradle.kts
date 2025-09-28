import io.typst.spigradle.spigot.paper
import io.typst.spigradle.spigot.papermc

plugins {
    kotlin("jvm") version "2.2.20"
    id("io.typst.spigradle") version "3.0.4"
}

group = "de.steffeeen"
version = "1.0"
val mcVersion = "1.21.8"
val stylizedName = "BiomeChanger"

repositories {
    mavenCentral()
    papermc()
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly(paper(mcVersion))
}

spigot {
    name.set(stylizedName)
    description.set("Adds a tool that allows you to change the biome of blocks.")
    libraries = listOf("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
    commands {
        create("biomechanger") {
            description = "Gives the BiomeChanger tool"
        }
    }
    apiVersion = mcVersion
}

tasks.jar {
    archiveBaseName.set(stylizedName)
    archiveAppendix.set("mc$mcVersion")
}