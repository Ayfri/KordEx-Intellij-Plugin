import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	java
	kotlin("jvm") version "1.7.20"
	id("org.jetbrains.intellij") version "1.9.0"
}

fun getEnv(envName: String) = System.getenv(envName)?.replace(Regex("\n+"), "")

group = "io.ayfri"
version = "0.5.1"

repositories {
	google()
	mavenCentral()
}

intellij {
	version.set("2022.3")
	type.set("IC")
	
	plugins.set(
		listOf(
			"java",
			"com.intellij.gradle",
			"com.intellij.properties",
			"org.jetbrains.kotlin",
		)
	)
}

tasks {
	withType<JavaCompile> {
		sourceCompatibility = "17"
		targetCompatibility = "17"
	}
	
	withType<KotlinCompile> {
		kotlinOptions.jvmTarget = "17"

		// Delete build/tmp to update plugin in Intellij sandbox
		delete {
			delete(file("build/tmp"))
		}
	}
	
	patchPluginXml {
		version.set(project.version.toString())
		changeNotes.set("""
			<![CDATA[
				${file("CHANGELOG.html").readText()}
			]]>
		""".trimIndent())
		sinceBuild.set("223")
		untilBuild.set("223.*")
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
