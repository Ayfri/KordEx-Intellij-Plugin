package io.ayfri.kordexplugin

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import io.ktor.util.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class CommandTranslationAnnotator : Annotator {
	override fun annotate(element: PsiElement, holder: AnnotationHolder) {
		val expression = when (element) {
			is KtCallExpression -> element
			else -> return
		}
		
		val module = expression.module ?: return
		val ktClass = expression.parentOfType<KtClass>() ?: return
		val className = ktClass.name ?: return
		
		val bundleName = cache.getOrPut(className) {
			PsiTreeUtil.findChildrenOfType(ktClass, KtProperty::class.java).firstOrNull {
				it.text.matches(Regex("override val bundle = .+"))
			}?.text?.substringAfter("= ")?.replace(Regex("^\"(.+)\"$"), "$1") ?: "kordex"
		}
		
		expression.resolveToCall(BodyResolveMode.PARTIAL)?.let { call ->
			val qualifiedName = call.resultingDescriptor.overriddenTreeUniqueAsSequence(true).last().fqNameSafe.asString()
			if (!CommandLineMarker.isValidKordExExpression(call) && TRANSLATE_FN_PATHS.none { it == qualifiedName }) return@let
			
			val expressionsToResolve = mutableListOf(expression.findName(), expression.findDescription())
			
			val methodName = call.resultingDescriptor.name.asString()
			expressionsToResolve += when (methodName) {
				"chatCommand" -> expression.findAliasKey()
				"translate", "respondTranslated" -> call.valueArgumentsByIndex?.get(0)?.arguments?.firstOrNull()?.getArgumentExpression()
				else -> null
			}
			
			expressionsToResolve.filterNotNull().forEach {
				createTranslationAnnotation(holder, module, it, bundleName, methodName)
			}
		}
	}
	
	companion object {
		const val BUNDLE_DOC_LINK = "https://kordex.kotlindiscord.com/en/concepts/i18n#using-your-bundles"
		
		val TRANSLATE_FN_PATHS = arrayOf(
			"com.kotlindiscord.kord.extensions.commands.CommandContext.translate",
			"com.kotlindiscord.kord.extensions.events.EventContext.translate",
			"com.kotlindiscord.kord.extensions.commands.chat.ChatCommandContext.respondTranslated",
		)
		
		private val cache = mutableMapOf<String, String>()
		
		fun createTranslationAnnotation(
			holder: AnnotationHolder,
			module: Module,
			expression: KtElement,
			bundleName: String,
			methodName: String?,
		) {
			var bundleDirectoryName = bundleName
			var bundleFileName = "strings.properties"
			
			if (bundleName.contains(".")) bundleName.partition {
				it == '.'
			}.let {
				bundleDirectoryName = it.first
				bundleFileName = "${it.second}.properties"
			}
			
			val foundProperty = module.searchPropertyInRB(expression.text.replace("\"", ""), bundleDirectoryName, bundleFileName)
			val tooltip =
				when {
					foundProperty != null ->
						"""
							Command translation got from <strong>${link(foundProperty.psiElement, "$bundleDirectoryName/$bundleFileName")}</strong> bundle file :
							
							<strong>${foundProperty.value?.escapeHTML() ?: "No value set."}</strong>
						"""
					
					methodName?.contains("translate", true) == true ->
						"""
							Command translation not found in <strong>$bundleDirectoryName/$bundleFileName</strong> bundle file.
							${if (bundleDirectoryName == "kordex") "<em>Maybe consider setting a <a href=$BUNDLE_DOC_LINK>bundle</a> name, <code>kordex</code> is the default one and should not be used.<em>" else ""}
						"""
					
					else -> null
				}?.trimIndent() ?: return
			
			holder.newAnnotation(
				HighlightSeverity.INFORMATION,
				"Command translation inferred",
			).tooltip(tooltip).let {
				if (tooltip.startsWith("Command translation not")) it.highlightType(ProblemHighlightType.WEAK_WARNING)
				else it
			}.needsUpdateOnTyping().range(expression).create()
		}
	}
}
