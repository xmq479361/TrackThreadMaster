package com.xmq.track.thread

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.xmq.track.thread.model.XThreadDelegate
import org.apache.commons.io.FileUtils
import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter

/**
 * @author xmqyeah* @CreateDate 2021/8/30 19:55
 */
class XTrackThreadTransform extends Transform {
    @Override
    String getName() {
        return "ThreadTrack"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return true
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        def startTime = System.currentTimeMillis()
        if (!transformInvocation.isIncremental()) {
            transformInvocation.getOutputProvider().deleteAll()
        }
        TrackThreadCore.instance.innerClassTo.clear()
//         TrackThreadCore.instance.replaces.clear()
        transformInvocation.inputs.each { transformInput ->
            transformInput.directoryInputs.each { dirInput ->
                def sourceFile = dirInput.getFile();
                String root = sourceFile.absolutePath
                def destDir = transformInvocation.getOutputProvider().getContentLocation(
                        dirInput.getName(), dirInput.getContentTypes(), dirInput.getScopes(), Format.DIRECTORY
                )
                println("find dir: ${sourceFile.name}, $destDir == ${dirInput.changedFiles.entries}")
                handleDirectory(root, sourceFile)
                println("copyDirectory: ${dirInput.file.path} => ${destDir.path}")
                FileUtils.copyDirectory(sourceFile, destDir)
            }
            transformInput.getJarInputs().each { jarInput ->
                def dest = transformInvocation.getOutputProvider().getContentLocation(
                        jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR
                )
                FileUtils.copyFile(jarInput.file, dest)
            }
        }
        println('Scan finish, current cost time ' + (System.currentTimeMillis() - startTime)
                + "ms")
    }

    void handleDirectory(String root, File sourceFile) {
        boolean leftSlash = File.separator == '/'
        sourceFile.listFiles(new FileFilter() {
            @Override
            boolean accept(File file) {
                return (file != null && (file.isDirectory() || file.name.endsWith(".class")))
            }
        }).each { file ->
            try {

                System.out.println("handleFile: ${file.absolutePath.replace(root, "")}," +
                        " filter: ${TrackThreadCore.isExclude(file.absolutePath.replace(root, ""))}")
                if (file.isDirectory()) {
                    handleDirectory(root, file)
                } else if (file.name.endsWith(".class")) {
                    if (!file.name.contains("BuildConfig") && !file.name.startsWith("R\$")) {
                        FileInputStream inputStream = null
                        try {
                            inputStream = new FileInputStream(file)
                            ClassReader cr = new ClassReader(inputStream)
                            ClassWriter cw = new ClassWriter(cr, 0)
                            TrackClassVisitor cv = new TrackClassVisitor(Opcodes.ASM5, cw)
                            cr.accept(cv, ClassReader.EXPAND_FRAMES)
                            FileUtils.writeByteArrayToFile(file, cw.toByteArray())
                        } finally {
                            if (inputStream != null) {
                                inputStream.close()
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace()
            } finally {
            }
        }
    }

    class TrackClassVisitor extends ClassVisitor {
        String source, className

        TrackClassVisitor(int api, ClassVisitor classVisitor) {
            super(api, classVisitor)
        }

        @Override
        void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            System.out.println("visitClass: " + name + " " + signature + "=" + superName)
            def superTypeTo = TrackThreadCore.instance.findTo(superName)
            if (superTypeTo != null) {
                System.out.println("===visitClass put: " + name + signature + "=" + superName + " ==> " + superTypeTo)
                TrackThreadCore.instance.innerClassTo.put(name, superTypeTo)
                superName = superTypeTo
            }
            className = name
            super.visit(version, access, name, signature, superName, interfaces)
        }

        @Override
        void visitSource(String source, String debug) {
            super.visitSource(source, debug)
            this.source = source
        }

        @Override
        MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions)

            System.out.println("visitMethod: " + access + ", " + name + " ==> " + descriptor + ", " +
                    TrackThreadCore.instance.findTo(name))
            return new TrackThreadMethodVisitor(api, mv, access, name, descriptor, className, source)
        }

    }

    class TrackThreadMethodVisitor extends AdviceAdapter {
        String className, source, methodName
        int lineNo

        protected TrackThreadMethodVisitor(int api, MethodVisitor methodVisitor, int access, String name,
                                           String descriptor, String className, String source) {
            super(api, methodVisitor, access, name, descriptor)
            this.methodName = name
            this.className = className
            this.source = source
        }

        @Override
        void visitLineNumber(int line, Label start) {
            super.visitLineNumber(line, start)
            lineNo = line
        }

        @Override
        void visitTypeInsn(int opcode, String type) {
            String typeTo = TrackThreadCore.getInstance().findTo(type)
            System.out.println("\t\tvisitTypeInsn: " + opcode + ", " + type + " => "
                    + typeTo + "== " + access)
            if (typeTo != null) { //
                type = typeTo
            }
            super.visitTypeInsn(opcode, type)
        }

        @Override
        void visitMaxs(int maxStack, int maxLocals) {
            System.out.println("\t\t\tvisitMaxs: " + access + ", " + lineNo + ", " + methodName
                    + ", $name, [$maxStack x $maxLocals]")
            super.visitMaxs(Math.min(minStack, maxStack), maxLocals)
        }
        int minStack = Integer.MAX_VALUE

        @Override
        void visitMethodInsn(int opcodeAndSource, String owner, String name, String descriptor, boolean isInterface) {
            String typeTo = TrackThreadCore.getInstance().findTo(owner)
            System.out.println("\t\tvisitMethodInsn: " + access + ", " + lineNo + ", " + opcodeAndSource
                    + ", " + owner + ", " + name + ", " + descriptor + ", typeTo: " + typeTo + " == " +
                    TrackThreadCore.instance.innerClassTo.containsKey(owner))
            // TODO 内部类不做处理
            if (!TrackThreadCore.instance.innerClassTo.containsKey(owner)) {
                XThreadDelegate delegate = TrackThreadCore.getInstance().find(owner)
                // 判断当前是否注入点
                if (delegate != null && name == delegate.method) {
                    int paramEndIndex = descriptor.indexOf(")")
                    String paramsAppend = descriptor.substring(0, paramEndIndex)
                    paramsAppend = paramsAppend + "Ljava/lang/String;" + descriptor.substring(paramEndIndex)
                    System.out.println("\t\t====Thread Replace: $owner: $name, $descriptor ==>$paramsAppend")
                    descriptor = paramsAppend
                    super.visitLdcInsn("(" + source + ":" + lineNo + ") " + methodName + "()")
                    owner = typeTo
                }
            }
//             String typeTo = TrackThreadCore.getInstance().findTo(owner)
//             System.out.println("\t\tvisitMethodInsn: "+access+", "+lineNo+", "+opcodeAndSource
//                     +", "+owner+", "+name+", "+descriptor+", typeTo: "+typeTo+" == "+
//                     TrackThreadCore.instance.innerClassTo.containsKey(owner))
//             if (name == "<init>" && TrackThreadCore.instance.innerClassTo.containsKey(owner)) {
////                 String paramsAppend = descriptor.substring(0, descriptor.length() - 2)
////                 paramsAppend = paramsAppend +"Ljava/lang/String;"+descriptor.substring(descriptor.length() - 2)
////                 System.out.println("\\t\\t====Thread Replace InnerClass: $owner: $name, $descriptor ==>$paramsAppend")
////                 descriptor = paramsAppend
////                 minStack = 4
////                 loadLocal(paramsTs.get(tmp--));
//             } else
//             if (name == "<init>" && TrackThreadCore.getInstance().containsKey(owner)) {
//                 String paramsAppend = descriptor.substring(0, descriptor.length() - 2)
//                 paramsAppend = paramsAppend +"Ljava/lang/String;"+descriptor.substring(descriptor.length() - 2)
//                 System.out.println("\t\t====Thread Replace: $owner: $name, $descriptor ==>$paramsAppend")
//                 descriptor = paramsAppend
//                 super.visitLdcInsn("("+source+":"+lineNo+") "+methodName+"()")
////                 def local = newLocal(Type.getType("java/lang/String"))
////                 storeLocal(local)
////                 loadLocal(paramsTs.get(tmp--));
////                 stackOffset++
//                 owner = typeTo
//             }
            super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
        }
    }
}
