plugins {
    java
    application
    kotlin("jvm") version "1.3.72"
    id("se.patrikerdes.use-latest-versions") version "0.2.14"
    id("com.github.ben-manes.versions") version "0.21.0"
//    id("com.github.johnrengelman.shadow") version "6.0.0"
}

group = "us.n8l"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}



java {
    modularity.inferModulePath.set(true)
}

dependencies {
    implementation(kotlin("stdlib"))
    testCompile("junit", "junit", "4.12")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.googlecode.lanterna:lanterna:3.0.1")
    implementation("commons-codec:commons-codec:1.11")
    implementation("joda-time:joda-time:2.10.1")
    implementation("org.apache.commons:commons-collections4:4.2")
    implementation("org.apache.commons:commons-lang3:3.8.1")
    // compile("com.googlecode.lanterna:lanterna:3.0.1")
    implementation("org.apache.logging.log4j:log4j-api:2.11.1")
    implementation("org.apache.logging.log4j:log4j-core:2.11.1")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.11.1")
    implementation("com.google.guava:guava:27.0.1-jre")
    implementation("org.jline:jline:3.9.0")
//    annotationProcessor "org.apache.logging.log4j:log4j-core:2.11.1")
}

application {
    mainModule.set("us.n8l.duplicatefinder") // name defined in module-info.java
    mainClass.set("us.n8l.duplicatefinder.Console")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
