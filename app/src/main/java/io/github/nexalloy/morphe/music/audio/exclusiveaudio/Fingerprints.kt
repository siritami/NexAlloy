package io.github.nexalloy.morphe.music.audio.exclusiveaudio

import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.opcodes
import org.luckypray.dexkit.result.MethodData
import java.lang.reflect.Modifier

private const val ENTITLEMENT_STRING = "probably_has_unlimited_entitlement"

private val UPSTREAM_OPCODES = listOf(
    Opcode.INVOKE_INTERFACE,
    Opcode.MOVE_RESULT_OBJECT,
    Opcode.IGET_OBJECT,
    Opcode.INVOKE_VIRTUAL,
    Opcode.MOVE_RESULT_OBJECT,
    Opcode.CHECK_CAST,
    Opcode.IF_NEZ,
    Opcode.IGET_OBJECT,
    Opcode.INVOKE_VIRTUAL,
    Opcode.MOVE_RESULT,
)

private fun MethodData.isEntitlementGetter() =
    returnTypeName == "boolean" &&
            paramCount == 0 &&
            Modifier.isPublic(modifiers) &&
            Modifier.isFinal(modifiers) &&
            !Modifier.isStatic(modifiers)

val AllowExclusiveAudioPlaybackFingerprint = findMethodDirect {
    val anchors = findMethod {
        matcher { addEqString(ENTITLEMENT_STRING) }
    }

    anchors.forEach { anchor ->
        val instructions = anchor.instructions
        val stringIndex = instructions.indexOfFirst { it.string == ENTITLEMENT_STRING }
        if (stringIndex < 0) return@forEach

        instructions.take(stringIndex)
            .lastOrNull { instruction ->
                val reference = instruction.methodRef
                reference != null && reference.returnTypeName == "boolean" && reference.paramCount == 0
            }
            ?.methodRef
            ?.let { return@findMethodDirect it }
    }

    val candidates = anchors.flatMap { it.invokes }
        .distinctBy { it.descriptor }
        .filter { it.isEntitlementGetter() }
    candidates.singleOrNull()?.let { return@findMethodDirect it }

    findMethod {
        matcher {
            modifiers = Modifier.PUBLIC or Modifier.FINAL
            returnType = "boolean"
            paramCount = 0
            opcodes(UPSTREAM_OPCODES)
        }
    }.distinctBy { it.descriptor }
        .singleOrNull()
        ?.let { return@findMethodDirect it }

    throw NoSuchElementException(
        "AllowExclusiveAudioPlaybackFingerprint unresolved. " +
                "anchors (${anchors.size}): ${anchors.joinToString { it.descriptor }} | " +
                "candidates (${candidates.size}): ${candidates.joinToString { it.descriptor }}"
    )
}
