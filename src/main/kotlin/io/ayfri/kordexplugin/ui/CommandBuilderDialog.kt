package io.ayfri.kordexplugin.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.listCellRenderer
import com.intellij.ui.layout.selectedValueIs
import io.ayfri.kordexplugin.utils.camelcase
import io.ayfri.kordexplugin.utils.titlecase

enum class CommandType {
	CHAT,
	EPHEMERAL_MESSAGE,
	PUBLIC_MESSAGE,
	EPHEMERAL_USER,
	PUBLIC_USER,
	EPHEMERAL_SLASH,
	PUBLIC_SLASH;

	fun fancyDisplayName() = name.replace("_"," ").titlecase()
	fun callName() = name.replace("_","").camelcase()

	companion object {
		val values = CommandType.values()
	}
}

data class CommandBuilderData(
	var name: String = "",
	var description: String = "",
	var type: CommandType = CommandType.CHAT,
	var aliases: MutableList<String> = mutableListOf(),
)

class CommandBuilderDialog : DialogWrapper(true) {
	init {
		title = "Command Builder"
		init()
	}

	private lateinit var nameField: JBTextField
	private lateinit var descriptionField: JBTextField
	private lateinit var typeField: ComboBox<CommandType>
	private lateinit var aliasesField: JBTextField

	var data
		get() = CommandBuilderData(
			nameField.text,
			descriptionField.text,
			typeField.selectedItem as CommandType,
			aliasesField.text.replace(Regex("\\s+"), "").split(',').filter { it.isNotBlank() }.toMutableList(),
		)
		set(value) {
			nameField.text = value.name
			descriptionField.text = value.description
			typeField.selectedItem = value.type
			aliasesField.text = value.aliases.joinToString(" ")
		}

	override fun createCenterPanel() = panel {
		row("Type:") {
			typeField = comboBox(CommandType.values.toList(), listCellRenderer { value, index, isSelected ->
				setText(value.fancyDisplayName())
			}).focused().component
		}

		row("Name:") {
			nameField = textField().validation {
				if (it.text.isBlank()) error("Name cannot be empty")
				if (it.text.contains(' ')) error("Name cannot contain spaces")
				if (it.text.contains(Regex("[^a-zA-Z0-9_]"))) error("Name can only contain alphanumeric characters and underscores")

				doValidate()
			}.component
		}

		row("Description:") {
			descriptionField = textField().validation {
				if (it.text.isBlank()) warning("Description should be provided").withOKEnabled()
				if (it.text.length > 100) error("Description cannot be longer than 100 characters")

				doValidate()
			}.component
		}

		row("Aliases:") {
			aliasesField = textField().validation {
				if (it.text.isBlank()) {
					warning("Aliases should be provided").withOKEnabled()
				} else {
					val aliases = it.text.split(',')

					for (alias in aliases) {
						if (alias.isBlank()) {
							error("Aliases cannot be empty")
							break
						}

						if (alias.contains(' ')) {
							error("Aliases cannot contain spaces")
							break
						}

						if (alias.contains(Regex("[^a-zA-Z0-9_]"))) {
							error("Aliases can only contain alphanumeric characters and underscores")
							break
						}
					}
				}

				doValidate()
			}.component
		}.rowComment("Separate aliases with a comma, spaces will be removed").visibleIf(typeField.selectedValueIs(CommandType.CHAT))
	}.apply {
		withPreferredWidth(400)
	}

	override fun getDimensionServiceKey() = "io.ayfri.kordexplugin.ui.CommandBuilderDialog"
}
