package io.ayfri.kordexplugin.utils

import com.intellij.psi.PsiElement

fun link(target: PsiElement, text: String = "") = "<a href=\"#navigation/${target.containingFile.virtualFile.path}:${target.textOffset}\">$text</a>"

fun String.titlecase() = split(" ").joinToString(" ") { word ->
	word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

fun String.camelcase() = split(" ").mapIndexed { index, word ->
	word.lowercase().replaceFirstChar { if (it.isLowerCase() && index != 0) it.titlecase() else it.toString() }
}.joinToString("")
