import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	java
	kotlin("jvm") version "1.6.21"
	id("org.jetbrains.intellij") version "1.6.0"
}

fun getEnv(envName: String) = System.getenv(envName)?.replace(Regex("\n+"), "")

group = "io.ayfri"
version = "0.2.0"

repositories {
	google()
	mavenCentral()
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
	
	withType<KotlinCompile> {
		kotlinOptions.jvmTarget = "11"
		
		// Delete build/tmp to update plugin in Intellij sandbox
		delete {
			delete(file("build/tmp"))
		}
	}
	
	patchPluginXml {
		version.set(project.version.toString())
		sinceBuild.set("212")
		untilBuild.set("222.*")
	}
	
	signPlugin {
		val certificate = File("keys/chain.crt").readText(Charsets.UTF_8)
		certificateChain.set(certificate)
		val key = File("keys/private.pem").readText(Charsets.UTF_8)
		privateKey.set(key)
		password.set(getEnv("PRIVATE_KEY_PASSWORD"))
	}
	
	publishPlugin {
		token.set(getEnv("PUBLISH_TOKEN"))
	}
}
