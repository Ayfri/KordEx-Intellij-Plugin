package io.ayfri.kordexplugin

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.typeUtil.getImmediateSuperclassNotAny

fun KtCallExpression.findName() = PsiTreeUtil.findChildrenOfAnyType(this, KtBinaryExpression::class.java).firstOrNull {
	it.operationToken == KtTokens.EQ && it.left?.text == "name"
}?.let {
	return@let it.right
}

fun KtCallExpression.findDescription() = PsiTreeUtil.findChildrenOfAnyType(this, KtBinaryExpression::class.java).firstOrNull {
	it.operationToken == KtTokens.EQ && it.left?.text == "description"
}?.let {
	return@let it.right
}

fun KtCallExpression.findAliasKey() = PsiTreeUtil.findChildrenOfAnyType(this, KtBinaryExpression::class.java).firstOrNull {
	it.operationToken == KtTokens.EQ && it.left?.text == "aliasKey"
}?.let {
	return@let it.right
}

private const val EXTENSION_CLASS = "com.kotlindiscord.kord.extensions.extensions.Extension"
private const val SLASH_COMMAND = "com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommand"
fun ResolvedCall<*>.isValidKordExExpression(): Boolean {
	val kotlinType = extensionReceiver?.type ?: return false
	val parentType = kotlinType.getImmediateSuperclassNotAny() ?: return false
	val method = resultingDescriptor.name.asString()
	
	if (!method.contains(Regex("command|event", RegexOption.IGNORE_CASE))) return false
	if (parentType.getJetTypeFqName(false) == EXTENSION_CLASS) return true
	
	return parentType.getJetTypeFqName(false) == SLASH_COMMAND
}

private const val ARGUMENTS_CLASS = "com.kotlindiscord.kord.extensions.commands.Arguments"
fun ResolvedCall<*>.isValidKordExArguments(): Boolean {
	val kotlinType = extensionReceiver?.type ?: return false
	val parentType = kotlinType.getImmediateSuperclassNotAny() ?: return false
	
	return parentType.getJetTypeFqName(false) == ARGUMENTS_CLASS
}