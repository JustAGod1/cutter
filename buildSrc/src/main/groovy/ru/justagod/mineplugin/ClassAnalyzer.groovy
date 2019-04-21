package ru.justagod.mineplugin

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Created by JustAGod on 02.03.2018.
 */
class ClassAnalyzer extends ClassVisitor {

    public final List<String> result = new ArrayList<>()
    public boolean dead = false
    private final GradleSide target
    private String currentMethod

    ClassAnalyzer(ClassVisitor visitor, GradleSide target) {
        super(Opcodes.ASM5, visitor)
        this.target = target
    }

    @Override
    AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc == "Lru/justagod/mineplugin/GradleSideOnly;") {
            return new ClassAnnotationAnalyzer()
        }
        return super.visitAnnotation(desc, visible)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        currentMethod = name + desc
        return new MethodAnalyzer(super.visitMethod(access, name, desc, signature, exceptions))
    }

    private class MethodAnalyzer extends MethodVisitor {

        MethodAnalyzer(MethodVisitor mv) {
            super(Opcodes.ASM5, mv)
        }

        @Override
        AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (desc == "Lru/justagod/mineplugin/GradleSideOnly;") {
                return new MethodAnnotationAnalyzer()
            }
            return super.visitAnnotation(desc, visible)
        }
    }

    private class MethodAnnotationAnalyzer extends AnnotationVisitor {

        MethodAnnotationAnalyzer() {
            super(Opcodes.ASM5, null)
        }



        @Override
        void visitEnum(String name, String desc, String value) {
            if (value == target.toString()) {
                result.add(currentMethod)
            }

        }
    }

    private class ClassAnnotationAnalyzer extends AnnotationVisitor {

        ClassAnnotationAnalyzer() {
            super(Opcodes.ASM5, null)
        }



        @Override
        void visitEnum(String name, String desc, String value) {
            if (value == target.toString()) {
                dead = true
            }

        }
    }

}

