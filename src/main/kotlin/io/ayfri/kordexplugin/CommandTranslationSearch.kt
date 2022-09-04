package io.ayfri.kordexplugin

import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.lang.properties.ResourceBundle
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope

/**
 * The cache of Ressource Bundles, mapped by their name.
 */
val cacheRB = mutableMapOf<String, ResourceBundle>()

fun Module.searchPropertyInRB(name: String, resourceBundleName: String, ressourceBundleFileName: String): IProperty? {
	val files = FileTypeIndex.getFiles(PropertiesFileType.INSTANCE, GlobalSearchScope.projectScope(project))
	
	val resourceBundle = cacheRB.getOrPut(resourceBundleName) {
		files.asSequence().map {
			it.psiFile()
		}.filter {
			this == it?.getModule()
		}.filterIsInstance<PropertiesFile>().map {
			it.resourceBundle
		}.firstOrNull {
			it.baseDirectory?.name == resourceBundleName
		} ?: return@searchPropertyInRB null
	}
	
	return resourceBundle.propertiesFiles.firstOrNull {
		it.virtualFile.name == ressourceBundleFileName
	}?.properties?.firstOrNull {
		it.key == name
	}
}

fun VirtualFile.psiFile(project: Project) = PsiManager.getInstance(project).findFile(this)

fun VirtualFile.guessProject() = ProjectLocator.getInstance().guessProjectForFile(this)

fun VirtualFile.psiFile() = guessProject()?.let { psiFile(it) }

fun PsiElement.getModule(): Module? {
	var module = ModuleUtilCore.findModuleForPsiElement(this)
	
	if (module == null) {
		val file = containingFile
		module = ModuleUtilCore.findModuleForPsiElement(file)
	}
	return module
}
