package io.ayfri.kordexplugin.features

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.properties.IProperty
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.findTopmostParentInFile
import com.intellij.util.containers.addAllIfNotNull
import io.ayfri.kordexplugin.*
import io.ayfri.kordexplugin.translations.cacheTranslations
import io.ayfri.kordexplugin.translations.searchPropertyInRB
import io.ayfri.kordexplugin.utils.link
import io.ktor.util.*
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class TranslationAnnotator : Annotator {
	override fun annotate(element: PsiElement, holder: AnnotationHolder) {
		val expression = when (element) {
			is KtCallExpression -> element
			else -> return
		}

		val module = expression.module ?: return
		val topLevelClass = expression.findTopmostParentInFile { it is KtClass } as? KtClass ?: return

		val bundleName =
			PsiTreeUtil.findChildrenOfType(topLevelClass, KtProperty::class.java).firstOrNull {
				it.text.matches(Regex("override val bundle = .+"))
			}?.text?.substringAfter("= ")?.replace(Regex("^\"(.+)\"$"), "$1") ?: "kordex"

		val call = expression.resolveToCall(BodyResolveMode.PARTIAL) ?: return
		val expressionsToResolve = mutableListOf<KtElement?>()
		val qualifiedName = call.resultingDescriptor.overriddenTreeUniqueAsSequence(true).last().fqNameSafe.asString()

		when {
			call.isValidKordExExpression() -> {
				expressionsToResolve.addAllIfNotNull(expression.findName(), expression.findDescription())
				if (qualifiedName.endsWith("chatCommand")) expressionsToResolve += expression.findAliasKey()
			}

			call.isValidKordExArguments() -> {
				expressionsToResolve.addAllIfNotNull(expression.findName(), expression.findDescription())
			}

			TRANSLATE_FN_PATHS.any { it == qualifiedName } -> {
				expressionsToResolve += call.valueArgumentsByIndex?.firstOrNull()?.arguments?.firstOrNull()?.getArgumentExpression()
			}

			else -> return
		}

		expressionsToResolve.filterNotNull().forEach {
			createTranslationAnnotation(holder, module, it, bundleName)
		}
	}

	companion object {
		const val BUNDLE_DOC_LINK = "https://kordex.kotlindiscord.com/en/concepts/i18n#using-your-bundles"

		val TRANSLATE_FN_PATHS = arrayOf(
			"com.kotlindiscord.kord.extensions.commands.CommandContext.translate",
			"com.kotlindiscord.kord.extensions.events.EventContext.translate",
			"com.kotlindiscord.kord.extensions.commands.chat.ChatCommandContext.respondTranslated",
		)

		fun createTranslationAnnotation(
			holder: AnnotationHolder,
			module: Module,
			expression: KtElement,
			bundleName: String,
		) {
			var bundleDirectoryName = bundleName
			var bundleFileName = "strings.properties"

			if (bundleName.contains(".")) bundleName.partition {
				it == '.'
			}.let {
				bundleDirectoryName = it.first
				bundleFileName = "${it.second}.properties"
			}

			val foundProperty = cacheTranslations.getOrElse(expression.text) {
				module.searchPropertyInRB(expression.text.replace("\"", ""), bundleDirectoryName, bundleFileName)?.also {
					cacheTranslations[expression.text] = it
				}
			}

			holder.createTranslationAnnotation(foundProperty, bundleDirectoryName, bundleFileName, /*methodName,*/ expression)
		}

		private fun AnnotationHolder.createTranslationAnnotation(
			foundProperty: IProperty?,
			bundleDirectoryName: String,
			bundleFileName: String,
			expression: KtElement,
		) {
			val bundlePath = "$bundleDirectoryName/$bundleFileName"
			val tooltip =
				when {
					foundProperty != null ->
						"""
							Command translation got from <strong>${link(foundProperty.psiElement, bundlePath)}</strong> bundle file :
							
							<strong>${foundProperty.value?.escapeHTML() ?: "No value set."}</strong>
						"""

					else -> """
								Command translation not found in <strong>$bundlePath</strong> bundle file.
								${if (bundleDirectoryName == "kordex") "<em>Maybe consider setting a <a href=$BUNDLE_DOC_LINK>bundle</a> name, <code>kordex</code> is the default one and should not be used.<em>" else ""}
							"""
				}.trimIndent()

			newAnnotation(
				HighlightSeverity.INFORMATION,
				"Command translation inferred",
			).tooltip(tooltip).let {
				if (tooltip.startsWith("Command translation not")) it.highlightType(ProblemHighlightType.WEAK_WARNING)
				else it
			}.needsUpdateOnTyping().range(expression).create()
		}
	}
}
