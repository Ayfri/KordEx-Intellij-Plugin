package io.ayfri.kordexplugin

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object Icons {
	private fun load(path: String) = IconLoader.getIcon(path, Icons::class.java)
	
	val LOGO = load("/images/logo.svg")
	val CHAT = load("/images/chat.svg")
	val GEAR_BLUE = load("/images/gear-blue.svg")
	val LIST_ORANGE = load("/images/list-orange.svg")
	val LIST_BLUE = load("/images/list-blue.svg")
	val MESSAGE_ORANGE = load("/images/message-orange.svg")
	val MESSAGE_BLUE = load("/images/message-blue.svg")
	val SLASH_ORANGE = load("/images/slash-orange.svg")
	val SLASH_BLUE = load("/images/slash-blue.svg")
	val SUB_ORANGE = load("/images/sub-orange.svg")
	val SUB_BLUE = load("/images/sub-blue.svg")
	val USER_ORANGE = load("/images/user-orange.svg")
	val USER_BLUE = load("/images/user-blue.svg")
	
	fun getIconForCommand(command: String): Icon = when {
		"user" in command -> if ("public" in command) USER_BLUE else USER_ORANGE
		"slash" in command -> if ("public" in command) SLASH_BLUE else SLASH_ORANGE
		"message" in command -> if ("public" in command) MESSAGE_BLUE else MESSAGE_ORANGE
		"group" in command -> if ("public" in command) LIST_BLUE else LIST_ORANGE
		"sub" in command -> if ("public" in command) SUB_BLUE else SUB_ORANGE
		"chat" in command -> CHAT
		else -> LOGO
	}
}
