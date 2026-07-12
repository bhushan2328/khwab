plugins {
    kotlin("jvm")
}

group = "com.toblad.khwab"
version = "0.1.0"


dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
