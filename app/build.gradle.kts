plugins {
 id("com.android.application")
 kotlin("android")
 kotlin("plugin.compose")
 id("com.google.devtools.ksp")
}
android {
 namespace = "br.com.pulsobateria"
 compileSdk = 35
 defaultConfig {
  applicationId = "br.com.pulsobateria"
  minSdk = 26
  targetSdk = 35
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  versionCode = 35
  versionName = "0.1.0-alpha.35"
 }
 buildFeatures { compose = true; buildConfig = true; aidl = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 // A mesma chave opcional em Debug e Release mantém a identidade entre builds do CI.
 // Sem Secrets, Debug ainda compila com a chave temporária padrão do Android.
 val stableFile = System.getenv("PULSO_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
 signingConfigs {
  if (stableFile != null) create("stable") {
   storeFile = file(stableFile)
   storePassword = System.getenv("KEYSTORE_PASSWORD")
   keyAlias = System.getenv("KEY_ALIAS")
   keyPassword = System.getenv("KEY_PASSWORD")
  }
 }
 buildTypes {
  getByName("debug") { if (stableFile != null) signingConfig = signingConfigs.getByName("stable") }
  getByName("release") {
   isMinifyEnabled = false
   if (stableFile != null) signingConfig = signingConfigs.getByName("stable")
  }
 }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
 implementation(project(":core"))
 implementation(platform("androidx.compose:compose-bom:2025.04.01"))
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-core")
 debugImplementation("androidx.compose.ui:ui-tooling")
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.core:core-ktx:1.16.0")
 implementation("androidx.core:core-splashscreen:1.0.1")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
 implementation("androidx.room:room-runtime:2.7.1")
 implementation("androidx.room:room-ktx:2.7.1")
 ksp("androidx.room:room-compiler:2.7.1")
 implementation("androidx.datastore:datastore-preferences:1.1.4")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.test:runner:1.6.2")
}
