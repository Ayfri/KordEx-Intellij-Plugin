package io.ayfri.kordexplugin.features

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.nextLeaf
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.extractMethod.newImpl.ExtractMethodHelper.addSiblingAfter
import io.ayfri.kordexplugin.EXTENSION_CLASS
import io.ayfri.kordexplugin.ui.CommandBuilderData
import io.ayfri.kordexplugin.ui.CommandBuilderDialog
import io.ayfri.kordexplugin.utils.findInsertAfterAnchor
import io.ayfri.kordexplugin.utils.getOrCreateBodyBlockExpression
import org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateActionBase
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.moveCaretIntoGeneratedElement
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.resolve.source.getPsi

class CommandBuilderAction : KotlinGenerateActionBase() {
	override fun invoke(project: Project, editor: Editor, file: PsiFile) {
		if (!EditorModificationUtil.checkModificationAllowed(editor)) return
		if (!FileDocumentManager.getInstance().requestWriting(editor.document, project)) return

		val builderDialog = CommandBuilderDialog()
		builderDialog.show()

		if (builderDialog.isOK) {
			val klass = getTargetClass(editor, file) ?: return
			val context = klass.analyzeWithContent()
			val classDescriptor = context.get(BindingContext.CLASS, klass) ?: return
			val targetClass = classDescriptor.source.getPsi() as? KtClass ?: return

			val currentMethod = file.findElementAt(editor.caretModel.offset)?.parentOfType<KtNamedFunction>() ?: return
			if (currentMethod.name != "setup") return

			return runWriteAction {
				val generatedCall = generateCommand(targetClass, builderDialog.data)
				val bodyBlockExpression = currentMethod.getOrCreateBodyBlockExpression()
				val findInsertAfterAnchor = findInsertAfterAnchor(editor, bodyBlockExpression) ?: return@runWriteAction

				val smartPointer = SmartPointerManager.createPointer(
					findInsertAfterAnchor.addSiblingAfter(generatedCall)
				)

				smartPointer.element?.addAfter(KtPsiFactory(project).createNewLine(), smartPointer.element?.lastChild)
				val kordExExpression = smartPointer.element as? KtCallExpression ?: return@runWriteAction
				ShortenReferences.DEFAULT.process(kordExExpression)

				val actionExpression = GoToActionAction.findActionElement(kordExExpression) ?: return@runWriteAction
				val insideActionExpression = actionExpression.findDescendantOfType<PsiWhiteSpace>()?.nextLeaf() ?: return@runWriteAction
				moveCaretIntoGeneratedElement(editor, insideActionExpression)
			}
		}
	}

	private fun generateCommand(targetClass: KtClass, data: CommandBuilderData): KtExpression {
		val psiFactory = KtPsiFactory(targetClass, true)
		val commandName = data.type.callName()
		val nameExpression = "name = \"${data.name}\""
		val descriptionExpression = data.description.takeIf { it.isNotBlank() }?.let { "description = \"$it\"" } ?: ""

		val aliasesExpression = data.aliases.takeIf { it.isNotEmpty() }?.let { aliases ->
			"aliases = arrayOf(${aliases.joinToString(", ") { "\"$it\"" }})"
		} ?: ""

		val commandExpression = "${commandName}Command"
		addImport(targetClass, commandExpression)

		return psiFactory.createExpression("""
			$commandExpression {
				$nameExpression
				$descriptionExpression
				${aliasesExpression + "\n"}
				action {
					
				}
			}
			""".trimIndent().replace(Regex("(\t+\n){2}"), " "))
	}

	val import = "com.kotlindiscord.kord.extensions.extensions."

	private fun addImport(targetClass: KtClass, functionName: String) {
		val psiFactory = KtPsiFactory(targetClass, true)
		val importStatement = psiFactory.createImportDirective(ImportPath.fromString("$import$functionName"))
		targetClass.containingKtFile.importList?.add(importStatement)
	}

	override fun isValidForClass(targetClass: KtClassOrObject) =
		targetClass.analyzeWithContent().get(BindingContext.CLASS, targetClass)?.getAllSuperclassesWithoutAny()
			?.any { it.fqNameOrNull()?.asString() == EXTENSION_CLASS } ?: false

}
