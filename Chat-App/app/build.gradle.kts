plugins {
    id("com.android.application")
}

class RoomSchemaArgProvider(
        @get:InputDirectory
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val schemaDir: File
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> {
        // Note: If you're using KSP, change the line below to return
        // listOf("room.schemaLocation=${schemaDir.path}").
        return listOf("-Aroom.schemaLocation=${schemaDir.path}")
    }
}

android {
    namespace = "edu.stevens.cs522.chat"
    compileSdk = 34

    defaultConfig {
        applicationId = "edu.stevens.cs522.chat"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                compilerArgumentProviders(
                        RoomSchemaArgProvider(File(projectDir, "schemas"))
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

// Hack to get around overlapping dependencies added by Kotlin libraries
configurations.implementation {
    exclude ("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
}

dependencies {

    // Material design (floating action button)
    implementation(libs.material)
    implementation(libs.appcompat)

    implementation(libs.preference)

    // Dependencies for RecyclerView
    implementation(libs.recyclerview)
    // For control over item selection of both touch and mouse driven selection
    implementation(libs.recyclerview.selection)

    // Dependencies for fragments (need FragmentActivity for LifeCycleOwner)
    implementation(libs.fragment)

    // Dependencies for LifeCycle, ViewModel, LiveData
    // ViewModel
    implementation(libs.lifecycle.viewmodel)
    // LiveData
    implementation(libs.lifecycle.livedata)
    // Lifecycles only (without ViewModel or LiveData)
    implementation(libs.lifecycle.runtime)

    // Saved state module for ViewModel
    implementation(libs.lifecycle.viewmodel.savedstate)

    // Annotation processor
    // annotationProcessor "androidx.lifecycle:lifecycle-compiler:$lifecycle_version"
    // alternately - if using Java8, use the following instead of lifecycle-compiler
    implementation(libs.lifecycle.common.java8)

    // optional - Test helpers for LiveData
    // testImplementation("androidx.arch.core:core-testing:$arch_version")


    // Dependencies for the Room ORM
    val room_version = "2.6.1"
    implementation(libs.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)

    // optional - Guava support for Room, including Optional and ListenableFuture
    implementation(libs.androidx.room.guava)

    implementation(files("libs/cs522-library.aar"))
    implementation(libs.guava)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}