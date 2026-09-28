/*
 * Copyright (c) 2024, LlemonDuck <napkinorton@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.ByteArrayOutputStream
import java.util.Properties

fun loadRootProperty(name: String): String? {
    val propsFile = file("../gradle.properties")
    if (!propsFile.isFile) {
        return null
    }
    val props = Properties()
    propsFile.inputStream().use { props.load(it) }
    return props.getProperty(name)
}

val projectxVersionProvider = providers.gradleProperty("projectx.version")
    .orElse(loadRootProperty("projectx.version") ?: "0.0.0")
val projectxPluginApiVersionProvider = providers.gradleProperty("projectx.pluginapi.version")
    .orElse(loadRootProperty("projectx.pluginapi.version") ?: "0.0.0")
val injectedClientVersionProvider = providers.gradleProperty("runelite.injected-client.version")
    .orElse(loadRootProperty("runelite.injected-client.version") ?: project.version.toString())

plugins {
    java
    `java-library`
    `maven-publish`
    pmd
    alias(libs.plugins.lombok)

    id("net.runelite.runelite-gradle-plugin.assemble")
    id("net.runelite.runelite-gradle-plugin.index")
    id("net.runelite.runelite-gradle-plugin.jarsign")

}

// Module-system flags required to extend com.apple.eawt.FullScreenAdapter on macOS
// (OSXFullScreenAdapter). Without these, the JVM throws IllegalAccessError at class load.
val macEawtJvmArgs = listOf(
    "--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED",
    "--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED",
    "--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED",
    "--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED"
)

tasks.register<JavaExec>("run") {
    group = "application"
    description = "Run RuneLite client"

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.runelite.client.RuneLite")

    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-ea"
    )
    jvmArgs(macEawtJvmArgs)
}

tasks.register<JavaExec>("seedMenuActionInfo") {
    group = "verification"
    description = "Generate src/main/resources/.../menu-action-info.properties from the injected-client jar. Re-run when the injected-client dependency bumps."

    dependsOn(":client:compileJava")

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.runelite.client.plugins.projectx.util.reflection.MenuActionResourceSeeder")

    val outputFile = file("src/main/resources/net/runelite/client/plugins/projectx/util/reflection/menu-action-info.properties")
    args(outputFile.absolutePath)

    doFirst {
        outputFile.parentFile.mkdirs()
    }
}

tasks.register<JavaExec>("runDebug") {
    group = "application"
    description = "Run RuneLite client with JDWP debug"

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.runelite.client.RuneLite")

    // same JVM args you need normally
    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-ea",
        // JDWP agent for debugger
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
    )
    jvmArgs(macEawtJvmArgs)
}

tasks.register<JavaExec>("runTest") {
    group = "verification"
    description = "Run client in test mode — auto-login, enable target plugin, write results, exit"

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.runelite.client.RuneLite")

    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-ea"
    )
    jvmArgs(macEawtJvmArgs)

    System.getProperties()
        .filter { it.key.toString().startsWith("projectx.test.") }
        .forEach { (k, v) -> jvmArgs("-D$k=$v") }
}

tasks.register<Test>("runDebugTests") {
    group = "verification"
    description = "Run tests with JDWP debug on port 5005 (attach debugger before tests run)"

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Duser.timezone=Europe/Brussels",
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
    )

    useJUnit()

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

tasks.register<Test>("runTests") {
    group = "verification"
    description = "Run tests with proper timezone (no debug)"

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Duser.timezone=Europe/Brussels"
    )

    useJUnit()

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

tasks.register<Test>("runUnitTests") {
    group = "verification"
    description = "Run unit tests only (no client, no login) — safe for CI"

    // ClientThreadGuardrailTest scans compiled .class files under runelite-{api,client}/build,
    // so make sure the main sources are compiled before the test JVM forks.
    dependsOn(":client:compileJava")

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Duser.timezone=Europe/Brussels",
        "-ea"
    )

    // Forward the baseline-regenerate flags to the test JVM so contributors can run
    // `./gradlew :client:runUnitTests -Dprojectx.guardrail.regenerate-baseline=true` ad-hoc.
    System.getProperty("projectx.guardrail.regenerate-baseline")?.let {
        systemProperty("projectx.guardrail.regenerate-baseline", it)
    }
    System.getProperty("projectx.queryable-guardrail.regenerate-baseline")?.let {
        systemProperty("projectx.queryable-guardrail.regenerate-baseline", it)
    }

    exclude("**/Rs2ActorModelIntegrationTest.class")
    exclude("**/Rs2WalkerIntegrationTest.class")
    exclude("**/Rs2ReflectionGroundItemActionsIntegrationTest.class")
    exclude("**/threadsafety/ClientThreadScannerTest.class")
    exclude("**/ScreenshotHandlerTest.class")

    useJUnit()

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.register<Test>("regenerateClientThreadGuardrailBaseline") {
    group = "verification"
    description = "Regenerate src/test/resources/threadsafety/client-thread-guardrail-baseline.txt from current sources"

    dependsOn(":client:compileJava", ":client:compileTestJava")

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Duser.timezone=Europe/Brussels",
        "-Dprojectx.guardrail.regenerate-baseline=true"
    )

    include("**/threadsafety/ClientThreadGuardrailTest.class")

    useJUnit()
    outputs.upToDateWhen { false }

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.register<Test>("regenerateQueryableTerminalBaseline") {
    group = "verification"
    description = "Regenerate src/test/resources/threadsafety/queryable-terminal-guardrail-baseline.txt from current sources"

    dependsOn(":client:compileJava", ":client:compileTestJava")

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Duser.timezone=Europe/Brussels",
        "-Dprojectx.queryable-guardrail.regenerate-baseline=true"
    )

    include("**/threadsafety/QueryableTerminalGuardrailTest.class")

    useJUnit()
    outputs.upToDateWhen { false }

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.register<Test>("runClientThreadScanner") {
    group = "verification"
    description = "Manually scan compiled bytecode for client-thread markers and emit docs/client-thread-manifest.md"
    enabled = true

    dependsOn(":client:compileJava", ":client:compileTestJava")

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Duser.timezone=Europe/Brussels",
        "-Dprojectx.scanner.enabled=true"
    )

    include("**/threadsafety/ClientThreadScannerTest.class")

    useJUnit()
    outputs.upToDateWhen { false }

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.register<Test>("runIntegrationTest") {
    group = "verification"
    description = "Run Rs2ActorModel integration test with live client"
    enabled = true

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Duser.timezone=Europe/Brussels",
        "-ea"
    )

    include("**/Rs2ActorModelIntegrationTest.class")
    include("**/Rs2WalkerIntegrationTest.class")

    useJUnit()

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

lombok.version = libs.versions.lombok.get()

java {
    withSourcesJar()
}

dependencies {
    api("net.runelite:runelite-api:${project.version}")
    implementation(project(":jshell"))
    runtimeOnly("net.runelite:injected-client:${injectedClientVersionProvider.get()}")

    api(libs.rl.http.api)
    api(libs.rl.discord)
    api(libs.rl.awt)
    compileOnly(libs.rl.orange)

    api(libs.slf4j.api)
    api(libs.logback.classic) {
        exclude("org.slf4j", "slf4j-api")
    }
    api(libs.jopt)
    implementation(libs.fastutil)
    api(libs.guava) {
        exclude("com.google.code.findbugs", "jsr305")
        exclude("com.google.errorprone", "error_prone_annotations")
        exclude("com.google.j2objc", "j2objc-annotations")
        exclude("org.codehaus.mojo", "animal-sniffer-annotations")
    }
    api(variantOf(libs.guice.core) { classifier("no_aop") }) {
        exclude("com.google.guava", "guava")
    }
    api(libs.gson)
    implementation(libs.jackson.databind)
    api(libs.flatlaf.core)
    implementation(libs.flatlaf.extras)
    api(libs.commons.text)
    api(libs.jna.core)
    api(libs.jna.platform)
    api(libs.findbugs)
    compileOnly(libs.jetbrains.annotations)
    api(libs.protobuf)
    api(libs.lwjgl.core)
    api(libs.lwjgl.opengl)
    api(libs.lwjgl.opencl)
    implementation(libs.asm.core)
    implementation(libs.asm.util)
    implementation(libs.asm.commons)

    for (platform in listOf(
        "linux",
        "linux-arm64",
        "macos",
        "macos-arm64",
        "windows-x86",
        "windows",
        "windows-arm64",
    )) {
        runtimeOnly(variantOf(libs.lwjgl.core) { classifier("natives-$platform") })
        runtimeOnly(variantOf(libs.lwjgl.opengl) { classifier("natives-$platform") })
    }

    testImplementation(libs.junit)
    testImplementation(libs.hamcrest)
    testImplementation(libs.mockito)
    testImplementation(libs.guice.testlib)
    testImplementation(libs.guice.grapher)
    testImplementation(libs.okhttp.mockserver)
}

val shadowJar = tasks.register<Jar>("shadowJar") {
    dependsOn(configurations.runtimeClasspath)
    manifest {
        attributes["Main-Class"] = "net.runelite.client.RuneLite"
        // Allows OSXFullScreenAdapter (which extends com.apple.eawt.FullScreenAdapter)
        // to load when launched via `java -jar`. JVM still requires --add-opens to
        // actually subclass the sealed class; manifest covers reflection access.
        attributes["Add-Opens"] = "java.desktop/com.apple.eawt java.desktop/com.apple.eawt.event"
        attributes["Add-Exports"] = "java.desktop/com.apple.eawt java.desktop/com.apple.eawt.event"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.map { it.map { if (it.isDirectory) it else zipTree(it) } })

    exclude(
        "META-INF/INDEX.LIST",
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "**/module-info.class"
    )

    group = BasePlugin.BUILD_GROUP
    archiveClassifier = "shadow"
    archiveFileName = project.name + "-" + project.version + "-shaded.jar"
}
tasks.assemble { dependsOn(shadowJar) }

publishing {
    publications {
        create<MavenPublication>("runelite-client") {
            from(components["java"])
            artifact(shadowJar) { classifier = "shaded" }
        }
    }
    repositories {
        val projectxRepoUrl = providers.gradleProperty("projectx.repo.url").orNull
        if (!projectxRepoUrl.isNullOrBlank()) {
            maven(uri(projectxRepoUrl)) {
                name = "projectx"
                credentials(PasswordCredentials::class) {
                    username = providers.gradleProperty("projectx.repo.username").getOrElse("")
                    password = providers.gradleProperty("projectx.repo.password").getOrElse("")
                }
            }
        }
    }
}

val assemble = tasks.withType<net.runelite.gradle.assemble.AssembleTask> {
    scriptDirectory = file("src/main/scripts")
    outputDirectory = sourceSets.main.map { File(it.output.resourcesDir, "runelite") }
    componentsFile = file("../runelite-api/src/main/interfaces/interfaces.toml")
    longSupport = true
}

tasks.withType<net.runelite.gradle.index.IndexTask> {
    archiveOverlayDirectory = assemble.single().outputDirectory
    indexFile = archiveOverlayDirectory.file("index")
}

tasks.processResources {
    inputs.property("projectVersion", project.version)

    val commit = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "--short=7", "HEAD")
        standardOutput = commit
    }

    val dirty = ByteArrayOutputStream()
    exec {
        commandLine("git", "status", "--short")
        standardOutput = dirty
    }

    val projectxVersion = projectxVersionProvider.get()
    val projectxCommit = providers.gradleProperty("projectx.commit.sha").getOrElse(commit.toString().trim())
    val projectxPluginApiVersion = projectxPluginApiVersionProvider.get()

    // Ensure task reruns when injected values change
    inputs.property("projectxVersion", projectxVersion)
    inputs.property("projectxCommit", projectxCommit)
    inputs.property("projectxPluginApiVersion", projectxPluginApiVersion)

    filesMatching("net/runelite/client/runelite.properties") {
        filter { it.replace("\${project.version}", project.version.toString()) }
        filter { it.replace("\${git.commit.id.abbrev}", commit.toString().trim()) }
        filter { it.replace("\${git.dirty}", dirty.toString().isNotBlank().toString()) }
        filter { it.replace("\${projectx.version}", projectxVersion) }
        filter { it.replace("\${projectx.commit.sha}", projectxCommit) }
        filter { it.replace("\${projectx.pluginapi.version}", projectxPluginApiVersion) }
    }
}

tasks.compileJava {
    options.isFork = true
}

tasks.jar {
    exclude("**/.clang-format")
}

val projectxReleaseJar = tasks.register<Copy>("projectxReleaseJar") {
    dependsOn(shadowJar)
    from(shadowJar.flatMap { it.archiveFile })
    into(layout.buildDirectory.dir("libs"))
    rename { "projectx-${projectxVersionProvider.get()}.jar" }
}

tasks.assemble { dependsOn(projectxReleaseJar) }

pmd {
    toolVersion = "7.2.0"
    ruleSetFiles("./pmd-ruleset.xml")
    isConsoleOutput = true
    incrementalAnalysis = true
    isIgnoreFailures = false
    threads = Runtime.getRuntime().availableProcessors()
}
tasks.pmdMain {
    exclude("**/RuntimeTypeAdapterFactory.java")
    exclude("**/net/runelite/client/party/Party.java")
}
tasks.pmdTest { enabled = false }

tasks.checkstyleMain {
    exclude("net/runelite/client/util/RuntimeTypeAdapterFactory.java") // vendored
    exclude("net/runelite/client/party/Party.java") // generated by protobuf
}

tasks.withType<Test> {
    if (name != "runIntegrationTest" && name != "runTests" && name != "runDebugTests" && name != "runUnitTests" && name != "runClientThreadScanner" && name != "regenerateClientThreadGuardrailBaseline" && name != "regenerateQueryableTerminalBaseline") {
        enabled = false
    }
    systemProperty("glslang.path", providers.gradleProperty("glslangPath").getOrElse(""))
}

tasks.javadoc {
    title = "RuneLite Client ${project.version} API"
}
