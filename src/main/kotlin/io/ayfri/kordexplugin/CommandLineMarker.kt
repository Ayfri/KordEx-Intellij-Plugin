package io.ayfri.kordexplugin

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.kotlindiscord.kord.extensions.extensions.Extension
import io.ayfri.kordexplugin.Icons.getIconForCommand
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.getImmediateSuperclassNotAny
import org.koin.ext.getFullName
import javax.swing.Icon


class CommandLineMarker : LineMarkerProviderDescriptor() {
	override fun getName() = "Command"
	
	override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
		val expression = when (element) {
			is KtCallExpression -> element
			else -> return null
		}
		
		if (expression.firstChild !is KtNameReferenceExpression) return null
		
		expression.resolveToCall(BodyResolveMode.PARTIAL)?.let {
			if (!isValidKordExExpression(it)) return@let
			val method = it.resultingDescriptor.name.asString()
			val icon = if (method == "event") Icons.GEAR_BLUE else getIconForCommand(method.lowercase())
			val identifier = expression.findDescendantOfType<PsiElement> { it.elementType == KtTokens.IDENTIFIER } ?: return@let
			
			return@getLineMarkerInfo gutter(identifier, icon, method) { _, _ ->
				GoToActionAction.moveCursorToAction(expression)
			}
		}
		
		return null
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
		fun isValidKordExExpression(call: ResolvedCall<*>): Boolean {
			val kotlinType = call.extensionReceiver?.type ?: return false
			val parentType = kotlinType.getImmediateSuperclassNotAny() ?: return false
			
			if (parentType.getJetTypeFqName(false) == Extension::class.getFullName()) return true
			
			return parentType.getJetTypeFqName(false) == "com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommand"
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
