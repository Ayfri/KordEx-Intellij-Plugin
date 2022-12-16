package io.ayfri.kordexplugin.features

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.jetbrains.rd.util.firstOrNull
import io.ayfri.kordexplugin.*
import io.ayfri.kordexplugin.Icons.getIconForCommand
import io.ayfri.kordexplugin.translations.cacheTranslations
import io.ayfri.kordexplugin.utils.link
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
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
			if (!call.isValidKordExExpression()) return@let
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
					gutterDescription = methodFancyDisplay

					expression.findName()?.let {
						gutterDescription += displayFancyLink("name", it)
					}

					expression.findDescription()?.let {
						gutterDescription += displayFancyLink("description", it)
					}

					if (method == "chatCommand") {
						expression.findAliasKey()?.let {
							gutterDescription += displayFancyLink("aliasKey", it)
						}
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
