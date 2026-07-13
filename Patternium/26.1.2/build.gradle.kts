plugins {
    id("net.fabricmc.fabric-loom") version "1.17.13"
    id("maven-publish")
}

version = property("mod_version") ?: "1.0.0"
group = property("maven_group") ?: "com.patternium"

repositories {
    mavenCentral()
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("patternium") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
}

tasks.processResources {
    val version: String = project.property("mod_version") as String
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.named("processClientResources", ProcessResources::class) {
    val version: String = project.property("mod_version") as String
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.withType<JavaCompile> {
    options.release = 25
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "Patternium"
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
    }
}
