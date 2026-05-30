plugins{
	java
	id("org.springframework.boot") version "3.3.4" apply false
	id("io.spring.dependency-management") version "1.1.6" apply false
}

allprojects{
	group = "com.telcoedge"
	version = "0.0.1-SNAPSHOT"
	
	repositories{
		mavenCentral()
	}
}

subprojects{
	plugins.apply("java")
	plugins.apply("io.spring.dependency-management")

	extensions.configure<JavaPluginExtension>{
		toolchain{
			languageVersion.set(JavaLanguageVersion.of(21))
		}
	}

	tasks.withType<Test>{
		useJUnitPlatform()
		jvmArgs("-Djdk.tracePinnedThreads=short")
		testLogging{
			events("passed", "failed", "skipped")
			showStandardStreams = true
		}
	}		
}
