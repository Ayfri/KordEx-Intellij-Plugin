package io.ayfri.kordexplugin.features

import com.intellij.featureStatistics.FeatureUsageTracker
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import io.ayfri.kordexplugin.isValidKordExExpression
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtCallExpression

class GoToActionAction : AnAction() {
	override fun actionPerformed(e: AnActionEvent) {
		val file = e.getRequiredData(CommonDataKeys.PSI_FILE)
		val caret = e.getRequiredData(CommonDataKeys.CARET)
		val psiElement = file.findElementAt(caret.offset) ?: return
		val commandExpression = psiElement.parentOfType<KtCallExpression>() ?: return

		commandExpression.resolveToCall()?.let {
			if (!it.isValidKordExExpression()) return@actionPerformed
		} ?: return

		moveCursorToAction(commandExpression)
	}

	override fun getActionUpdateThread() = ActionUpdateThread.BGT

	override fun update(e: AnActionEvent) {
		val file = e.getData(CommonDataKeys.PSI_FILE)
		val caret = e.getData(CommonDataKeys.CARET)

		e.presentation.isEnabledAndVisible = file?.findElementAt(caret?.offset ?: return) != null
	}

	companion object {
		fun findActionElement(kordExExpression: KtCallExpression) =
			PsiTreeUtil.findChildrenOfAnyType(kordExExpression, true, KtCallExpression::class.java).firstOrNull {
				it.firstChild?.text == "action"
			}

		fun moveCursorToAction(commandExpression: KtCallExpression) {
			val action = findActionElement(commandExpression) ?: return
			action.navigate(true)
			FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration")
		}
	}
}
