package ru.justagod.model.factory

import ru.justagod.model.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import ru.justagod.mincer.util.NodesFactory

class BytecodeModelFactory(private val harvester: NodesFactory) : ModelFactory {


    override fun makeModel(type: ClassTypeReference, parent: AbstractModel?): ClassModel {
        val node = harvester.makeNode(type)
        return makeModel(node, parent)
    }

    companion object {
        private val genericsRegex = "^<.*>".toRegex()

        fun makeModel(node: ClassNode, parent: AbstractModel?): InternalClassModel {
            val invisibleAnnotations = node.invisibleAnnotations
                    ?.groupBy { fetchTypeReference(it.desc) as ClassTypeReference }
                    ?.mapValues { it.value[0] }
                    ?.mapValues {
                        it.value.values?.chunked(2)?.map { Pair(it[0] as String, it[1]) }?.toMap() ?: emptyMap()
                    }
                    ?.mapValues {
                        it.value.mapValues {
                            @Suppress("IMPLICIT_CAST_TO_ANY")
                            if (it.value is Array<*>)
                                EnumHolder(fetchTypeReference((it.value as Array<String>)[0]) as ClassTypeReference, (it.value as Array<String>)[1])
                            else
                                it.value
                        }
                    }
                    ?: emptyMap()
            val visibleAnnotations = node.visibleAnnotations
                    ?.groupBy { fetchTypeReference(it.desc) as ClassTypeReference }
                    ?.mapValues { it.value[0] }
                    ?.mapValues {
                        it.value.values?.chunked(2)?.map { Pair(it[0] as String, it[1]) }?.toMap() ?: emptyMap()
                    }
                    ?.mapValues {
                        it.value.mapValues {
                            @Suppress("IMPLICIT_CAST_TO_ANY")
                            if (it.value is Array<*>)
                                EnumHolder(fetchTypeReference((it.value as Array<String>)[0]) as ClassTypeReference, (it.value as Array<String>)[1])
                            else
                                it.value
                        }
                    }
                    ?: emptyMap()
            val access = AccessModel(node.access)
            val hasDefaultConstructor = node.methods
                    ?.any { it.name == "<init>" && it.access and Opcodes.ACC_PUBLIC != 0 && it.parameters?.isEmpty() ?: true }
                    ?: false
            val enum = (node.access and Opcodes.ACC_ENUM != 0) && node.superName == "java/lang/Enum"
            val model = InternalClassModel(
                    parent,
                    visibleAnnotations,
                    invisibleAnnotations,
                    hasDefaultConstructor,
                    enum,
                    access,
                    ClassTypeReference(node.name.toCanonicalName())
            )
            val fields = node.fields?.map { makeFieldModel(it, model) } ?: emptyList()
            val methods = emptyList<MethodModel>()
            model._fields = fields
            model._methods = methods
            model._typeParameters = { parseClassSignature(node.signature, model).filterIsInstance<ReferencedGenericTypeModel>() }
            val (superClass, interfaces) = if (node.signature != null) {
                val elements = parseClassParentsSignature(node.signature, model)
                Pair(elements.firstOrNull(), elements.subList(1, elements.size))
            } else {
                Pair(node.superName?.let { FinalTypeModel(ClassTypeReference(it.toCanonicalName()), model) }, node.interfaces
                        ?.map { FinalTypeModel(ClassTypeReference(it.toCanonicalName()), model) })
            }
            model._superClass = superClass
            model._interfaces = interfaces
            return model
        }

        @Suppress("UNCHECKED_CAST")
        private fun parseClassParentsSignature(signature: String, parent: AbstractModel): List<ClassParent> {
            return parseGenericsArray("<" + getSignatureParents(signature) + ">", parent) as List<ClassParent>
        }

        private fun getSignatureParents(signature: String): String {
            if (signature.startsWith("<")) {
                var opening = 1
                for (i in 1 until signature.length) {
                    val char = signature[i]
                    if (char == '<') opening++
                    if (char == '>') opening--
                    if (opening <= 0) return signature.substring(i + 1)
                }
                error("")
            } else return signature
        }

        private fun parseClassSignature(signature: String?, parent: AbstractModel): List<TypeModel> {
            signature ?: return emptyList()
            return if (signature.contains(genericsRegex)) {
                val inner = signature.substring(1, signature.indexOf(">"))
                inner.split(";").filterNot { it.isEmpty() }.map { parseClassGeneric("$it;", parent) }
            } else emptyList()
        }

        private fun parseClassGeneric(signature: String, parent: AbstractModel): ReferencedGenericTypeModel {
            val name = signature.substring(0, signature.indexOf(":"))
            val bound = parseType("L" + signature.substring(signature.indexOf(":") + 2), parent)
            return ReferencedGenericTypeModel(name, bound, parent)
        }

        private fun makeFieldModel(field: FieldNode, parent: AbstractModel): FieldModel {
            val type = if (field.signature != null) parseSignature(field.signature, parent)
            else parseType(field.desc, parent)
            return FieldModel(
                    field.name,
                    type,
                    AccessModel(field.access),
                    (field.invisibleAnnotations
                            ?: emptyList<AnnotationNode>()).any { it.desc == "Lorg/jetbrains/annotations/Nullable;" },
                    parent
            )
        }

        private fun parseSignature(signature: String, parent: AbstractModel, arrayed: Boolean = false): TypeModel {
            return if (signature.startsWith("L"))
                if (signature.contains("<.*>".toRegex())) {
                    parseParameterizedType(signature, parent)
                } else {
                    parseType(signature, parent)
                }
            else if (signature == "*") WildcardGenericTypeModel(FinalTypeModel(ClassTypeReference("java.lang.Object"), parent), parent)
            else if (signature.startsWith("T")) parseGeneric(signature, parent)
            else if (signature.startsWith("[")) parseArrayGeneric(signature, parent)
            else if (signature.startsWith("-") || signature.startsWith("+")) parseWildcard(signature, parent)
            else if (arrayed) parseGeneric(signature, parent)
            else error("")
        }

        private fun parseWildcard(signature: String, parent: AbstractModel): WildcardGenericTypeModel {
            return if (signature.startsWith("-")) WildcardGenericTypeModel(FinalTypeModel(ClassTypeReference("java.lang.Object"), parent), parent)
            else WildcardGenericTypeModel(parseSignature(signature.substring(1), parent), parent)
        }

        private fun parseType(signature: String, parent: AbstractModel): TypeModel {
            val reference = fetchTypeReference(signature)
            return FinalTypeModel(reference, parent)
        }

        private fun parseArrayGeneric(signature: String, parent: AbstractModel): TypeModel {
            val type = parseSignature(signature.substring(1), parent, true)
            return ArrayGenericTypeModel(type, parent)
        }

        private fun parseGeneric(signature: String, parent: AbstractModel) =
                ReferencedGenericTypeModel(signature.substring(1 until signature.length - 1), null, parent)

        private fun parseParameterizedType(signature: String, parent: AbstractModel): TypeModel {
            val type = fetchTypeReference(signature.substring(0, signature.indexOf("<")) + ";") as ClassTypeReference

            return ParameterizedTypeModel(
                    type,
                    { parseGenericsArray(signature.substring(signature.indexOf("<") until signature.length - 1), parent) },
                    parent
            )
        }

        private fun parseGenericsArray(signature: String, parent: AbstractModel): List<TypeModel> {
            val inner = signature.drop(1).dropLast(1)
            val generics = gentleSplit(inner)
            return generics.filter { it.isNotEmpty() }.map {
                parseSignature(if (it == "*" || it.last() == ';') it else "$it;", parent)
            }
        }

        private fun gentleSplit(data: String): ArrayList<String> {
            val result = arrayListOf<String>()
            var opening = 0
            var builder = StringBuilder()
            for (char in data) {
                if (char == '<') opening++
                if (char == '>') opening--
                builder.append(char)
                if (char == ';' && opening <= 0) {
                    result += builder.toString()
                    builder = StringBuilder()
                }
            }
            result += builder.toString()
            return result
        }

        private fun String.toCanonicalName() = this.replace("/", ".")

    }

    class BytecodeNotFoundException(className: String) : Exception(className)

    data class EnumHolder(val type: ClassTypeReference, val value: String)

}