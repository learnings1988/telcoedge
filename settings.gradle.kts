rootProject.name = "telcoedge"

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

include(
	"common",
	"core-domain",
	"subscriber-api"
)