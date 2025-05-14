plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.kevmo314.kineticstreamer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kevmo314.kineticstreamer"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Configure native build ABIs
        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        aidl = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // Don't strip native libraries during APK build
        jniLibs {
            keepDebugSymbols += "**/*.so"
            useLegacyPackaging = true
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets {
        getByName("main") {

            jniLibs.setSrcDirs(listOf("src/main/jniLibs", "build/intermediates/cmake/debug/obj"))
        }
    }
}

// Create a custom task to build the Go library
tasks.register<Exec>("buildGoLibrary") {
    // Use a task output directory for the Go AAR
    val libDir = layout.buildDirectory.dir("go-lib").get().asFile
    mkdir(libDir)
    
    workingDir = file("src/main/go")
    // Check if we're on Windows
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    val scriptFile = if (isWindows) "build.bat" else "build.sh"

    val scriptPath = file("src/main/go/$scriptFile").absolutePath

    if (isWindows) {
        commandLine("cmd", "/c", scriptPath, libDir.absolutePath,)
    } else {
        commandLine("sh", scriptPath, libDir.absolutePath, )
    }
    
    doLast {
        // Create the libs directory if it doesn't exist
        mkdir("src/main/libs")
        
        // Copy the AAR to the libs directory
        copy {
            from(libDir)
            include("*.aar")
            into("src/main/libs")
        }
        println("Built and copied Go AAR to src/main/libs")
    }
}

// Run buildGoLibrary after the native libraries are built
// Make the mergeDebugJniLibFolders task depend on buildGoLibrary
afterEvaluate {
    tasks.named("mergeDebugJniLibFolders").configure {
        dependsOn("buildGoLibrary")
    }
    
    // Make buildGoLibrary depend on the C++ build
    tasks.named("buildGoLibrary").configure {
        dependsOn("externalNativeBuildDebug")
    }

    // Make Kotlin compile tasks depend on buildGoLibrary
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        dependsOn("buildGoLibrary")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.camera:camera-core:1.3.3")
    implementation("androidx.camera:camera-camera2:1.3.3")
    implementation("androidx.camera:camera-lifecycle:1.3.3")
    implementation("androidx.camera:camera-video:1.3.3")
    implementation("androidx.camera:camera-view:1.3.3")
    implementation("androidx.camera:camera-extensions:1.3.3")

    implementation("androidx.concurrent:concurrent-futures:1.1.0")

    implementation("androidx.datastore:datastore-preferences:1.1.0")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation(platform("androidx.compose:compose-bom:2024.04.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // Include AAR files from src/main/libs directory
    implementation(fileTree(mapOf(
        "dir" to "src/main/libs",
        "include" to listOf("*.aar", "*.jar"),
    )))
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.04.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}