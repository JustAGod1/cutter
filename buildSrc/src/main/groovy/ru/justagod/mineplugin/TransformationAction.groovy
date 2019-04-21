package ru.justagod.mineplugin

import jdk.internal.org.objectweb.asm.Opcodes
import org.gradle.api.Action
import org.gradle.api.Task
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

import java.util.function.Consumer
import java.util.function.Predicate
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Created by JustAGod on 01.03.2018.
 */
class TransformationAction implements Action<Task> {

    private final GradleSide target

    TransformationAction(GradleSide target) {
        this.target = target
    }

    @Override
    void execute(Task task) {
        for (File file : task.project.jar.outputs.getFiles().files) {
            def buffer = new ByteArrayOutputStream()
            def zout = new ZipOutputStream(buffer)

            JarFile jar = new JarFile(file)
            jar.stream().forEach { entry ->
                    if (!entry.isDirectory()) {
                        InputStream input = jar.getInputStream(entry)
                        byte[] bytes = new byte[(int) entry.size]
                        int read = 0
                        while (read < entry.size) {
                            read += input.read(bytes, read, (int) entry.size - read)
                        }

                        if (entry.name.endsWith(".class")) {
                            ClassReader cr = new ClassReader(bytes)
                            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
                                @Override
                                protected String getCommonSuperClass(String type1, String type2) {
                                    return "java/lang/Object"
                                }
                            }
                            ClassAnalyzer analyzer = new ClassAnalyzer(cw, target == GradleSide.SERVER ? GradleSide.CLIENT : GradleSide.SERVER)
                            try {
                                cr.accept(analyzer, ClassReader.SKIP_FRAMES)
                            } catch (Exception e) {
                                println "Exception while processing class " + entry.name
                                zout.putNextEntry(new ZipEntry(entry.name))
                                zout.write(bytes)
                                zout.closeEntry()
                                input.close()
                                return
                            }

                            if (!analyzer.dead) {
                                if (!analyzer.result.isEmpty()) {
                                    zout.putNextEntry(new ZipEntry(entry.name))
                                    cr = new ClassReader(cw.toByteArray())

                                    ClassNode classNode = new ClassNode(Opcodes.ASM5)
                                    cr.accept(classNode, ClassReader.SKIP_FRAMES)

                                    ((List<MethodNode>) classNode.methods).removeIf(new Predicate<MethodNode>() {
                                        @Override
                                        boolean test(MethodNode node) {
                                            String info = node.name + node.desc
                                            boolean result = analyzer.result.contains(info)
                                            if (result) {
                                                println(info + " has been discarded")
                                            }
                                            return result
                                        }
                                    })

                                    cw = new ClassWriter(ClassWriter.COMPUTE_MAXS)
                                    classNode.accept(cw)
                                    zout.write(cw.toByteArray())
                                    zout.closeEntry()
                                } else {
                                    zout.putNextEntry(new ZipEntry(entry.name))
                                    zout.write(bytes)
                                    zout.closeEntry()
                                }
                            } else {
                                println(entry.name + " has been discarded")
                            }
                        } else {
                            zout.putNextEntry(new ZipEntry(entry.name))
                            zout.write(bytes)
                            zout.closeEntry()
                        }
                        input.close()
                    } else {
                        zout.putNextEntry(new ZipEntry(entry.name))
                        zout.closeEntry()
                    }

            }
            zout.close()
            jar.close()

            file.delete()

            String name = file.absolutePath.substring(0, file.absolutePath.lastIndexOf('.')) + '-' + target.postfix + ".jar"
            File targetFile = new File(name)
            if (!targetFile.exists()) targetFile.createNewFile()

            FileOutputStream out = new FileOutputStream(targetFile)
            out.write(buffer.toByteArray())
            out.close()
        }
    }
}
