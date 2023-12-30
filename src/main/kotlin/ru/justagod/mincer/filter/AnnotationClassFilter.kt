package ru.justagod.mincer.filter

import org.objectweb.asm.ClassReader
import ru.justagod.mincer.Mincer
import ru.justagod.model.ClassTypeReference

class AnnotationClassFilter(private val target: ClassTypeReference) : ClassFilter {
    override fun isValid(name: ClassTypeReference, mincer: Mincer): Boolean {
        val classNode = mincer.makeNode(name, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        return classNode.invisibleAnnotations != null && classNode.invisibleAnnotations.any {
            ClassTypeReference.fromDesc(it.desc) == target
        }
    }
}