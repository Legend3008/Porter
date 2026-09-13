plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.porter.core.ui"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.13" }
}
dependencies {
    api(project(":core:designsystem"))
    api(project(":core:common"))
    api(project(":domain:model"))
    implementation(libs.core.ktx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.lottie.compose)
    implementation(libs.accompanist.systemuicontroller)
}
