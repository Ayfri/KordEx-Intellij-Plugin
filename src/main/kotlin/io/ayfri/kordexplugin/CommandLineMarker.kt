package io.ayfri.kordexplugin

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.kotlindiscord.kord.extensions.extensions.Extension
import io.ayfri.kordexplugin.Icons.getIconForCommand
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.getImmediateSuperclassNotAny
import javax.swing.Icon


class CommandLineMarker : LineMarkerProviderDescriptor() {
	override fun getName() = "Command"
	
	override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
		val expression = when (element) {
			is KtCallExpression -> element
			else -> return null
		}
		
		if (expression.firstChild !is KtNameReferenceExpression) return null
		
		expression.resolveToCall(BodyResolveMode.PARTIAL)?.let { call ->
			if (!isValidKordExExpression(call)) return@let
			val method = call.resultingDescriptor.name.asString()
			val icon = if (method == "event") Icons.GEAR_BLUE else getIconForCommand(method.lowercase())
			val identifier = expression.findDescendantOfType<PsiElement> { it.elementType == KtTokens.IDENTIFIER } ?: return@let
			
			val methodFancyDisplay = method.replace(Regex("([a-z](?=[A-Z]))"), "$1 ").replace(Regex("(\\b[a-z])")) { it.value.uppercase() }
			val name = expression.findName()
			val description = expression.findDescription()
			
			var gutterDescription = methodFancyDisplay
			
			name?.let {
				gutterDescription += "\n${link(name, "name")} = ${it.text}"
			}
			
			description?.let {
				gutterDescription += "\n${link(description, "description")} = ${it.text}"
			}
			
			GoToActionAction.findActionElement(expression)?.let { action ->
				gutterDescription += "\n\n${link(action, "Go to Action")}"
			}
			
			return@getLineMarkerInfo gutter(identifier, icon, gutterDescription) { _, _ ->
				GoToActionAction.moveCursorToAction(expression)
			}
		}
		
		return null
	}
	
	fun KtCallExpression.findName(): KtExpression? {
		return PsiTreeUtil.findChildrenOfAnyType(this, KtBinaryExpression::class.java).firstOrNull {
			logger.info("searching name: ${it.text}")
			it.operationToken == KtTokens.EQ && it.left?.text == "name"
		}?.let {
			return@let it.right
		}
	}
	
	fun KtCallExpression.findDescription(): KtExpression? {
		return PsiTreeUtil.findChildrenOfAnyType(this, KtBinaryExpression::class.java).firstOrNull {
			it.operationToken == KtTokens.EQ && it.left?.text == "description"
		}?.let {
			return@let it.right
		}
	}
	
	fun <T : PsiElement> gutter(
		expression: T,
		icon: Icon,
		method: String,
		onClick: GutterIconNavigationHandler<T> = GutterIconNavigationHandler { _, _ ->  },
	): LineMarkerInfo<T> {
		return ActionnableLineMarkerInfo(
			expression,
			icon,
			{ method },
			{ method },
			onClick = onClick,
		)
	}
	
	companion object {
		private const val EXTENSION_CLASS = "com.kotlindiscord.kord.extensions.extensions.Extension"
		private const val SLASH_COMMAND = "com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommand"
		
		fun isValidKordExExpression(call: ResolvedCall<*>): Boolean {
			val kotlinType = call.extensionReceiver?.type ?: return false
			val parentType = kotlinType.getImmediateSuperclassNotAny() ?: return false
			val method = call.resultingDescriptor.name.asString()
			
			if (!method.contains(Regex("command|event", RegexOption.IGNORE_CASE))) return false
			
			if (parentType.getJetTypeFqName(false) == EXTENSION_CLASS) return true
			
			return parentType.getJetTypeFqName(false) == SLASH_COMMAND
		}
	}
}

class ActionnableLineMarkerInfo<T : PsiElement>(
	element: T,
	icon: Icon,
	tooltip: ((T) -> String)?,
	name: () -> String,
	alignement: GutterIconRenderer.Alignment = GutterIconRenderer.Alignment.RIGHT,
	onClick: GutterIconNavigationHandler<T>,
) : LineMarkerInfo<T>(
	element,
	element.textRange,
	icon,
	tooltip,
	onClick,
	alignement,
	name
)
