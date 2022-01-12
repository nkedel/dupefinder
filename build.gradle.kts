plugins {
    java
    application
    kotlin("jvm") version "1.6.10"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"
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
    val log4j2Version = "2.17.1"
    testImplementation("junit", "junit", "4.13.2")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.googlecode.lanterna:lanterna:3.1.0")
    implementation("commons-codec:commons-codec:1.15")
    implementation("joda-time:joda-time:2.10.13")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.logging.log4j","log4j-api", log4j2Version)
    implementation("org.apache.logging.log4j","log4j-core", log4j2Version)
    implementation("org.apache.logging.log4j","log4j-slf4j-impl", log4j2Version)
    implementation("org.jline:jline:3.21.0")
//    annotationProcessor "org.apache.logging.log4j:log4j-core"+log4j2Version)
}

application {
    mainModule.set("us.n8l.duplicatefinder") // name defined in module-info.java
    mainClass.set("us.n8l.duplicatefinder.Console")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}