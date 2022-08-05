package io.ayfri.kordexplugin

import com.intellij.psi.PsiElement

fun link(target: PsiElement, text: String = "") = "<a href=\"#navigation/${target.containingFile.virtualFile.path}:${target.textOffset}\">$text</a>"

fun link(file: String, offset: Int, text: String = "") = "<a href=\"#navigation/$file:$offset\">$text</a>"