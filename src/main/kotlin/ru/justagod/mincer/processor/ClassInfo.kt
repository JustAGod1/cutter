package ru.justagod.mincer.processor

import org.objectweb.asm.tree.ClassNode
import ru.justagod.model.ClassModel

class ClassInfo(val data: ClassModel, val node: ClassNode)