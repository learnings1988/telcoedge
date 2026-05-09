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
	apply(plugin = "java")
	apply(plugin = "io.spring.dependency-management")
	
	extensions.configure<JavaPluginExtension>{
		toolchain{
			languageVersion.set(JavaLanguageVersion.of(25))
			vendor.set(JvmVendorSpec.ADOPTIUM)
		}
	}

	tasks.withType<Test>{
		useJUnitPlatform()
	}		
}
