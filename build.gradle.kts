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
        classpath("com.github.jengelman.gradle.plugins:shadow:4.0.4")
        classpath("com.github.ben-manes:gradle-versions-plugin:0.20.0")
    }

//    plugins {
//      id "net.ltgt.apt" version "0.18"
//    }


}
repositories {
    jcenter()
    mavenCentral()
}

plugins {
    java
    application
    kotlin("jvm") version "1.3.20"
}

apply {
    plugin("com.github.johnrengelman.shadow")
    plugin("com.github.ben-manes.versions")
//    plugin("net.ltgt.apt")
}

application {
    mainClassName = "us.n8l.duplicatefinder.Console"
}

dependencies {
    compile(kotlin("stdlib"))
    compile("com.google.code.findbugs:jsr305:3.0.2")
    compile("com.googlecode.lanterna:lanterna:3.0.1")
    compile("commons-codec:commons-codec:1.11")
    compile("joda-time:joda-time:2.10.1")
    compile("org.apache.commons:commons-collections4:4.2")
    compile("org.apache.commons:commons-lang3:3.8.1")
    // compile("com.googlecode.lanterna:lanterna:3.0.1")
    compile(files("lib/lanterna-3.1.0-SNAPSHOT.jar", "lib/lanterna-native-integration-3.0.0-SNAPSHOT.jar"))
    compile("org.apache.logging.log4j:log4j-api:2.11.1")
    compile("org.apache.logging.log4j:log4j-core:2.11.1")
    compile("org.apache.logging.log4j:log4j-slf4j-impl:2.11.1")
    compile("com.google.guava:guava:27.0.1-jre")
    compile("org.jline:jline:3.9.0")
//    annotationProcessor "org.apache.logging.log4j:log4j-core:2.11.1")
    testCompile("junit:junit:4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
