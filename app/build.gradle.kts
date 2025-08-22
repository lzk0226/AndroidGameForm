plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.app.gameform"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.gameform"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.scenecore)
    implementation(libs.common)
    implementation(libs.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.gson)

    implementation("com.github.bumptech.glide:glide:4.14.2")

    // CircleImageView for user avatars
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // RecyclerView (if not already included)
    implementation("androidx.recyclerview:recyclerview:1.3.0")

    // CardView (if not already included)
    implementation("androidx.cardview:cardview:1.0.0")

    implementation("com.github.bumptech.glide:glide:4.14.2")

    // CircleImageView for user avatars
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.0")

    // CardView
    implementation("androidx.cardview:cardview:1.0.0")

    // OkHttp for HTTP networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}