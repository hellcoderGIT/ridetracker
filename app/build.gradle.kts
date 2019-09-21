import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

plugins {
	id("com.android.application")
	id("org.jetbrains.dokka-android")
	id("com.google.gms.oss.licenses.plugin")
	id("io.fabric")
	Dependencies.corePlugins(this)
}


if (file("key.gradle").exists()) {
	apply("key.gradle")
}

android {
	compileSdkVersion(Android.compile)
	buildToolsVersion(Android.buildTools)
	defaultConfig {
		applicationId = "com.adsamcik.signalcollector"
		minSdkVersion(Android.min)
		targetSdkVersion(Android.target)
		versionCode = 323
		versionName = "2019.1ι10"
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		resConfigs("en", "cs")
	}

	with(compileOptions) {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}

	tasks.withType<KotlinCompile> {
			with(kotlinOptions) {
			jvmTarget = "1.8"
			freeCompilerArgs = listOf("-Xuse-experimental=kotlin.ExperimentalUnsignedTypes")
		}
	}

	buildTypes {
		getByName("debug") {
			isTestCoverageEnabled = true
		}

		create("release_nominify") {
			isMinifyEnabled = false
		}
		getByName("release") {
			isMinifyEnabled = true
			proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
		}
	}

	lintOptions {
		isCheckReleaseBuilds = true
		isAbortOnError = false
	}

	sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")

	packagingOptions {
		pickFirst("META-INF/atomicfu.kotlin_module")
	}

	dynamicFeatures = mutableSetOf(":statistics", ":game", ":map")


}

tasks.withType<DokkaTask> {
	outputFormat = "html"
	outputDirectory = "$buildDir/javadoc"
	jdkVersion = 8
	skipEmptyPackages = true
	skipDeprecated = true

	externalDocumentationLink {
		url = URL("https://developer.android.com/reference/")
		packageListUrl = URL("https://developer.android.com/reference/android/support/package-list")
	}
}

dependencies {
	implementation(project(":common"))
	implementation(project(":tracker"))
	implementation(project(":activity"))

	Dependencies.core(this)
	//1st party dependencies
	implementation("com.adsamcik.android-components:slider:0.8.0")
	Dependencies.draggable(this)

	Dependencies.introduction(this)

	//3rd party dependencies
	Dependencies.moshi(this)

	Dependencies.colorChooser(this)
	Dependencies.fileChooser(this)

	Dependencies.gpx(this)

	//Google dependencies
	implementation("androidx.cardview:cardview:1.0.0")

	//Preference
	Dependencies.preference(this)
	implementation("de.psdev.licensesdialog:licensesdialog:2.1.0")

	//PlayServices
	Dependencies.location(this)
	Dependencies.crashlytics(this)

	//Database
	Dependencies.database(this)


	Dependencies.test(this)
	//workaround  Multiple APKs packaging the same library can cause runtime errors.
	implementation(project(":commonmap"))
	Dependencies.map(this)
}
apply {
	plugin("com.google.gms.google-services")
}
