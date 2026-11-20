import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val generatedLauncherResources = layout.buildDirectory.dir("generated/launcher-icons/res")
val launcherIconSource = layout.projectDirectory.file("src/main/assets/launcher-icon.txt")

android {
    sourceSets.getByName("main").res.srcDir(generatedLauncherResources)
    namespace = "com.granit.survival"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.granit.survival"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }


    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}

// Generate density-specific Android launcher icons from the authored 48x48 pixel-art master.
// Nearest-neighbour scaling preserves hard pixel edges at every launcher density.
val generateLauncherIcons = tasks.register("generateLauncherIcons") {
    inputs.file(launcherIconSource)
    outputs.dir(generatedLauncherResources)
    doLast {
        val lines = launcherIconSource.asFile.readLines().filter { it.isNotBlank() && !it.startsWith("#") }
        require(lines.size == 2) { "launcher-icon.txt must contain a palette line and a pixel-data line" }
        val palette = lines[0].split(",").map { it.trim() }
        val pixels = lines[1].trim()
        val alphabet = "0123456789abcdefghijklmnopqrstuv"
        require(palette.size == 32) { "Launcher icon palette must contain exactly 32 colors" }
        require(pixels.length == 48 * 48) { "Launcher icon must contain exactly 2304 pixels" }
        val root = generatedLauncherResources.get().asFile
        val densitySizes = mapOf("mipmap-mdpi" to 48, "mipmap-hdpi" to 72, "mipmap-xhdpi" to 96, "mipmap-xxhdpi" to 144, "mipmap-xxxhdpi" to 192)
        densitySizes.forEach { (folder, size) ->
            val directory = File(root, folder).apply { mkdirs() }
            val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until size) for (x in 0 until size) {
                val sourceIndex = (y * 48 / size) * 48 + (x * 48 / size)
                val paletteIndex = alphabet.indexOf(pixels[sourceIndex])
                require(paletteIndex in palette.indices) { "Invalid launcher icon palette index at $sourceIndex" }
                val rgb = palette[paletteIndex].removePrefix("#").toLong(16).toInt() or (0xFF shl 24)
                image.setRGB(x, y, rgb)
            }
            ImageIO.write(image, "png", File(directory, "ic_launcher.png"))
            ImageIO.write(image, "png", File(directory, "ic_launcher_foreground.png"))
            ImageIO.write(image, "png", File(directory, "ic_launcher_round.png"))
        }
        val adaptiveDirectory = File(root, "mipmap-anydpi-v26").apply { mkdirs() }
        val adaptiveXml = "<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">\n    <background android:drawable=\"@color/ic_launcher_background\" />\n    <foreground android:drawable=\"@mipmap/ic_launcher_foreground\" />\n</adaptive-icon>\n"
        File(adaptiveDirectory, "ic_launcher.xml").writeText(adaptiveXml)
        File(adaptiveDirectory, "ic_launcher_round.xml").writeText(adaptiveXml)
        val valuesDirectory = File(root, "values").apply { mkdirs() }
        File(valuesDirectory, "ic_launcher_background.xml").writeText("<resources><color name=\"ic_launcher_background\">#101A28</color></resources>")
    }
}
tasks.named("preBuild").configure { dependsOn(generateLauncherIcons) }
tasks.configureEach { if (name.startsWith("merge") && name.endsWith("Resources")) dependsOn(generateLauncherIcons) }
