import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.internal.os.OperatingSystem

plugins {
	java
	kotlin("jvm") version "1.6.21"
	id("org.jetbrains.intellij") version "1.5.2"
}

fun getEnv(envName: String) = System.getenv(envName)?.replace(Regex("\n+"), "")

group = "io.ayfri"
version = "0.1.1"

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
	implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.5.3-20220422.152027-6") {
		version {
			require("1.5.3-20220422.152027-6")
		}
	}
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
	
	buildSearchableOptions {
		// https://youtrack.jetbrains.com/issue/IDEA-210683
		jvmArgs(
			"--illegal-access=deny",
			"--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED",
			"--add-opens=java.base/java.lang=ALL-UNNAMED",
			"--add-opens=java.base/java.util=ALL-UNNAMED",
			"--add-opens=java.desktop/java.awt.event=ALL-UNNAMED",
			"--add-opens=java.desktop/java.awt=ALL-UNNAMED",
			"--add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED",
			"--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
			"--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
			"--add-opens=java.desktop/sun.font=ALL-UNNAMED",
			"--add-opens=java.desktop/sun.swing=ALL-UNNAMED"
		)
		
		if (OperatingSystem.current().isMacOsX) {
			jvmArgs("--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED")
		}
	}
}
