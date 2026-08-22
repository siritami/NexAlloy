package io.github.nexalloy.morphe.music.layout.upgradebutton

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.fingerprint

internal val pivotBarConstructorFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    returns("V")
    parameters("L", "Z")
    opcodes(
        Opcode.INVOKE_INTERFACE,
        Opcode.GOTO,
        Opcode.IPUT_OBJECT,
        Opcode.RETURN_VOID
    )
}

val pivotBarElementField = findFieldDirect {
    val constructor = pivotBarConstructorFingerprint()
    val declaringClass = constructor.declaredClass
        ?: throw NoSuchElementException("pivotBarConstructorFingerprint has no declaring class")

    constructor.instructions
        .lastOrNull { it.opcode == Opcode.IPUT_OBJECT.opCode }
        ?.fieldRef
        ?.let { return@findFieldDirect it }

    val listFields = declaringClass.fields.filter { it.typeName == "java.util.List" }
    listFields.singleOrNull()
        ?: throw NoSuchElementException(
            "pivotBarElementField unresolved in ${declaringClass.descriptor}. " +
                    "List fields (${listFields.size}): ${listFields.joinToString { it.descriptor }}"
        )
}
