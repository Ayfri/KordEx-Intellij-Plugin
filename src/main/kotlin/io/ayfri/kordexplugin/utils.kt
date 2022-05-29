package io.ayfri.kordexplugin

import com.intellij.psi.PsiElement

fun link(target: PsiElement, text: String = ""): String {
	return "<a href=\"#navigation/${target.containingFile.virtualFile.path}:${target.textOffset}\">$text</a>"
}
