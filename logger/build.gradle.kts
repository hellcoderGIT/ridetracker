import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("com.android.library")
	Dependencies.corePlugins(this)
}

android {
	compileSdkVersion(Android.compile)
	buildToolsVersion(Android.buildTools)

	defaultConfig {
		minSdkVersion(Android.min)
		targetSdkVersion(Android.target)
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	sourceSets {
		this.maybeCreate("androidTest").assets.srcDirs(files("$projectDir/schemas"))
	}

	compileOptions {
		isCoreLibraryDesugaringEnabled = true
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
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
		}
	}

	lintOptions {
		isCheckReleaseBuilds = true
		isAbortOnError = false
	}

	kapt {
		arguments {
			this.arg("room.schemaLocation", "$projectDir/schemas")
		}
	}
	tasks.withType<KotlinCompile> {
		with(kotlinOptions) {
			jvmTarget = "1.8"
		}
	}
}

dependencies {
	implementation(project(":sbase"))
	implementation(project(":spreferences"))

	Dependencies.core(this)
	Dependencies.database(this)
	Dependencies.crashlytics(this)
	Dependencies.test(this)
}