plugins {
	java
	kotlin("jvm") version "1.6.21"
	id("org.jetbrains.intellij") version "1.5.3"
}

group = "io.ayfri"
version = "1.0-SNAPSHOT"

repositories {
	google()
	mavenCentral()
	
	maven {
		name = "Sonatype Snapshots"
		url = uri("https://oss.sonatype.org/content/repositories/snapshots")
	}
	
	maven {
		name = "Kotlin Discord"
		url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
	}
}

dependencies {
	implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.5.3-20220422.152027-6")
}

intellij {
	version.set("2022.1")
	type.set("IC")
	
	plugins.set(listOf(
		"java",
		"com.intellij.gradle",
		"org.jetbrains.kotlin",
	))
}

tasks {
	withType<JavaCompile> {
		sourceCompatibility = "11"
		targetCompatibility = "11"
	}
	withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
		kotlinOptions.jvmTarget = "11"
	}
	
	patchPluginXml {
		sinceBuild.set("212")
		untilBuild.set("222.*")
	}
	
	signPlugin {
		certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
		privateKey.set(System.getenv("PRIVATE_KEY"))
		password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
	}
	
	publishPlugin {
		token.set(System.getenv("PUBLISH_TOKEN"))
	}
}
