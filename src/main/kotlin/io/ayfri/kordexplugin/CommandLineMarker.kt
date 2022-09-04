package io.ayfri.kordexplugin

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.jetbrains.rd.util.firstOrNull
import io.ayfri.kordexplugin.Icons.getIconForCommand
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
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
	
	private fun displayFancyLink(name: String, target: KtExpression) = "\n${link(target, name)} = ${cacheTranslations[target.text]?.value ?: target.text}"
	
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
			
			var gutterDescription: String
			
			when (method) {
				"event" -> {
					val firstTypeParameter = call.typeArguments.firstOrNull()?.value ?: return@let
					val genericMethodType = firstTypeParameter.fqName?.pathSegments()?.lastOrNull()?.asString() ?: return@let
					val genericTypeFancyDisplay = genericMethodType.replace("Event", " Event")
					
					gutterDescription = genericTypeFancyDisplay
				}
				
				else -> {
					val methodFancyDisplay = method.replace(Regex("([a-z](?=[A-Z]))"), "$1 ").replace(Regex("(\\b[a-z])")) { it.value.uppercase() }
					val name = expression.findName()
					val description = expression.findDescription()
					
					gutterDescription = methodFancyDisplay
					
					name?.let {
						gutterDescription += displayFancyLink("name", it)
					}
					
					description?.let {
						gutterDescription += displayFancyLink("description", it)
					}
				}
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
	
	fun <T : PsiElement> gutter(
		expression: T,
		icon: Icon,
		method: String,
		onClick: GutterIconNavigationHandler<T> = GutterIconNavigationHandler { _, _ -> },
	) = ActionnableLineMarkerInfo(
		expression,
		icon,
		{ method },
		{ method },
		onClick = onClick,
	)
	
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

fun KtCallExpression.findName() = PsiTreeUtil.findChildrenOfAnyType(this, KtBinaryExpression::class.java).firstOrNull {
	it.operationToken == KtTokens.EQ && it.left?.text == "name"
}?.let {
	return@let it.right
}

fun KtCallExpression.findDescription() = PsiTreeUtil.findChildrenOfAnyType(this, KtBinaryExpression::class.java).firstOrNull {
	it.operationToken == KtTokens.EQ && it.left?.text == "description"
}?.let {
	return@let it.right
}

fun KtCallExpression.findAliasKey() = PsiTreeUtil.findChildrenOfAnyType(this, KtBinaryExpression::class.java).firstOrNull {
	it.operationToken == KtTokens.EQ && it.left?.text == "aliasKey"
}?.let {
	return@let it.right
}