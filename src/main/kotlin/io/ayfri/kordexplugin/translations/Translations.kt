package io.ayfri.kordexplugin.translations

import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

/**
 * Cache for translations, mapped by "key" -> "property".
 */
val cacheTranslations = mutableMapOf<String, IProperty>()

data class TranslationEvent(val fileEvent: VFileEvent, val file: PropertiesFile)

class TranslationsListener : BulkFileListener {
	override fun after(events: MutableList<out VFileEvent>) {
		
		val translationEvents = events.map { fileEvent ->
			if (fileEvent.requestor !is FileDocumentManagerImpl) return@map null
			
			fileEvent.file?.let {
				cacheRB.values.forEach { ressourceBundle ->
					ressourceBundle.propertiesFiles.find {
						it.virtualFile.path == fileEvent.file?.path
					}?.let {
						return@map TranslationEvent(fileEvent, it)
					}
				}
				
				null
			}
		}.filterNotNull()
		
		if (translationEvents.isEmpty()) return
		
		translationEvents.forEach { translationEvent ->
			val file = translationEvent.file
			val fileEvent = translationEvent.fileEvent
			
			if (fileEvent.isFromSave) {
				file.properties.forEach { property ->
					if (property.key != null) {
						cacheTranslations[property.key!!] = property
					}
				}
			}
		}
	}
}