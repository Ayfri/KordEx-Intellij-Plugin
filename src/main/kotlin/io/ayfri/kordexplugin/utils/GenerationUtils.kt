package io.ayfri.kordexplugin.utils

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.siblings

internal fun KtNamedFunction.getOrCreateBodyBlockExpression() = bodyBlockExpression ?: runWriteAction {
	val blockExpression = KtPsiFactory(this).createEmptyBody()
	add(blockExpression)
	blockExpression
}

internal fun removeAfterOffset(offset: Int, whiteSpace: PsiWhiteSpace): PsiElement {
	// Check if the given offset is within the text range of the whitespace character
	if (!whiteSpace.textRange.contains(offset)) {
		return whiteSpace
	}

	val spaceNode = whiteSpace.node
	var beforeWhiteSpaceText = spaceNode.text.substring(0, offset - spaceNode.startOffset)
	if (!StringUtil.containsLineBreak(beforeWhiteSpaceText)) {
		// Prevent insertion on same line
		beforeWhiteSpaceText += "\n"
	}

	val factory = KtPsiFactory(whiteSpace.project)

	val insertAfter = whiteSpace.prevSibling ?: return whiteSpace

	whiteSpace.delete()

	val beforeSpace = factory.createWhiteSpace(beforeWhiteSpaceText)
	insertAfter.parent.addAfter(beforeSpace, insertAfter)

	return insertAfter.nextSibling
}

internal fun findInsertAfterAnchor(editor: Editor, body: KtBlockExpression): PsiElement? {
	val lBrace = body.lBrace ?: return null

	val offset = editor.caretModel.offset
	val offsetCursorElement = PsiTreeUtil.findFirstParent(body.containingFile.findElementAt(offset)) {
		it.parent == body
	}

	if (offsetCursorElement is PsiWhiteSpace) {
		return removeAfterOffset(offset, offsetCursorElement)
	}

	if (offsetCursorElement != null && offsetCursorElement.parent == body) {
		return offsetCursorElement
	}

	val comment = lBrace
		.siblings(withItself = false)
		.takeWhile { it is PsiWhiteSpace || it is PsiComment }
		.lastOrNull { it is PsiComment }

	return comment ?: lBrace
}

/*
internal fun findInsertAfterAnchor(editor: Editor, parent: PsiElement): PsiElement? {
	// Find the caret position and the element at the caret position
	val caretModel = editor.caretModel
	val offset = caretModel.offset
	val elementAtCaret = parent.findElementAt(offset)

	// Check if the element at the caret position is a valid anchor
	if (elementAtCaret != null && isValidAnchor(elementAtCaret)) {
		// Check if the element at the caret position is a child of the parent
		if (elementAtCaret.getTreeParent() == parent) {
			// Return the element at the caret position as the anchor
			return elementAtCaret
		} else {
			// The element at the caret position is not a child of the parent
			// Return null to indicate that there is no valid anchor
			return null
		}
	} else {
		// The element at the caret position is not a valid anchor
		// Return null to indicate that there is no valid anchor
		return null
	}
}*/
