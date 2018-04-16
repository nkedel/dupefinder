import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.benmanes.gradle.versions.VersionsPlugin

group = "us.n8l"
version = "0.1.0-SNAPSHOT"

buildscript {
    repositories {
        jcenter()
        mavenCentral()
        maven {
            setUrl("https://plugins.gradle.org/m2/")
        }
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:2.0.3")
        classpath("com.github.ben-manes:gradle-versions-plugin:0.17.0")
    }

}
repositories {
    jcenter()
    mavenCentral()
}

plugins {
    java
    application
    kotlin("jvm") version "1.2.31"
}

apply {
    plugin("com.github.johnrengelman.shadow")
    plugin("com.github.ben-manes.versions")
}

application {
    mainClassName = "us.n8l.duplicatefinder.Console"
}

dependencies {
    compile(kotlin("stdlib"))
    compile("com.googlecode.lanterna:lanterna:3.0.0")
    compile("joda-time:joda-time:2.9.9")
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_9
    targetCompatibility = JavaVersion.VERSION_1_9
}