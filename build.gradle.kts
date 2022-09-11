import kr.entree.spigradle.kotlin.jitpack
import kr.entree.spigradle.kotlin.paper

plugins {
    idea
    kotlin("jvm") version "1.7.10"
    id("kr.entree.spigradle") version "2.4.2"
}

group = "de.steffeeen"
version = "1.0"
val mcVersion = "1.19.2"
val stylizedName = "BiomeChanger"

repositories {
    mavenCentral()
    jitpack()
}

dependencies {
    compileOnly(paper(mcVersion))
    implementation(kotlin("stdlib-jdk8"))
}

spigot {
    name = stylizedName
    description = "Adds a tool that allows you to change the biome of blocks."
    commands {
        create("biomechanger") {
            description = "Gives the BiomeChanger tool"
        }
    }
    apiVersion = "1.19"
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

tasks.jar {
    archiveBaseName.set(stylizedName)
    archiveAppendix.set("mc$mcVersion")
}