package com.xmq.track.thread

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import groovyjarjarasm.asm.Type
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

/**
 * @author xmqyeah
 * @CreateDate 2021/8/30 19:55
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
         transformInvocation.inputs.each {transformInput ->
             transformInput.directoryInputs.each{ dirInput ->
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
             transformInput.getJarInputs().each {jarInput ->
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
         }).each {file->
             try {

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

         boolean isExtendsOfThread = false

         @Override
         void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
             System.out.println("visitClass: "+name+" "+signature+"="+superName)
             if (TrackThreadCore.instance.containsKey(superName)) {
                 System.out.println("visitClass put: "+name+signature+"="+superName +" ==> "+
                         TrackThreadCore.instance.findTo(superName))
                 isExtendsOfThread = true
                 TrackThreadCore.instance.put(name, TrackThreadCore.instance.findTo(superName))
//                 superName = TrackThreadCore.instance.findTo(superName)
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

             System.out.println("visitMethod: "+access+", "+name +" ==> "+descriptor+", "+
                     TrackThreadCore.instance.findTo(name))
             return new TrackThreadMethodVisitor(api, mv, access, name, descriptor, className, source, isExtendsOfThread)
         }

//         @Override
//         void visitInnerClass(String name, String outerName, String innerName, int access) {
//             super.visitInnerClass(name, outerName, innerName, access)
//             System.out.println("\t\tvisitInnerClass: "+name+", "+outerName+" => "
//                     +innerName+", "+access)
//         }
     }

     class TrackThreadMethodVisitor extends AdviceAdapter {
        String className, source, methodName
         int lineNo
         boolean isExtendsOfThread
         protected TrackThreadMethodVisitor(int api, MethodVisitor methodVisitor, int access, String name,
                                            String descriptor, String className, String source, isExtendsOfThread) {
             super(api, methodVisitor, access, name, descriptor)
             this.methodName = name
             this.className = className
             this.source = source
             this.isExtendsOfThread = isExtendsOfThread
         }

         @Override
         void visitLineNumber(int line, Label start) {
             super.visitLineNumber(line, start)
             lineNo = line
         }

         @Override
         void visitTypeInsn(int opcode, String type) {
             System.out.println("\t\tvisitTypeInsn: "+opcode+", "+type+" => "
                     +TrackThreadCore.getInstance().findTo(type)+"== "+access)
             String typeTo = TrackThreadCore.getInstance().findTo(type)
             if (typeTo != null && ACC_PUBLIC == access){
                if (opcode == NEW) {
                    super.visitTypeInsn(NEW, type)
                    super.visitInsn(DUP);
                }
                 super.visitTypeInsn(opcode, type)
                 return
             }
//             Opcodes.NEW
             if (TrackThreadCore.getInstance().containsKey(type)) {
                 type = TrackThreadCore.getInstance().findTo(type)
             }
             super.visitTypeInsn(opcode, type)
         }

         @Override
         void visitMethodInsn(int opcodeAndSource, String owner, String name, String descriptor, boolean isInterface) {
             System.out.println("\t\tvisitMethodInsn: "+lineNo+", "+opcodeAndSource+", "+owner+", "+name+", "+descriptor)

             String typeTo = TrackThreadCore.getInstance().findTo(owner)
             if (typeTo != null && ACC_PUBLIC == access) {
                 if (opcodeAndSource == INVOKESPECIAL) {
                     super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
                     super.visitLdcInsn("("+source+":"+lineNo+") "+methodName+"()")
                     super.visitMethodInsn(opcodeAndSource, typeTo, name,
                             "(Ljava/lang/Runnable;Ljava/lang/String;)V", isInterface)
                     return
                 }
             }
             if (name == "<init>" && TrackThreadCore.getInstance().containsKey(owner)) {
                 String paramsAppend = descriptor.substring(0, descriptor.length() - 2)
                 paramsAppend = paramsAppend +"Ljava/lang/String;"+descriptor.substring(descriptor.length() - 2);
                 descriptor = paramsAppend;
                 super.visitLdcInsn("("+source+":"+lineNo+") "+methodName+"()")
//                 def local = newLocal(Type.getType("java/lang/String"))
//                 storeLocal(local)
//                 loadLocal(paramsTs.get(tmp--));
                 owner = TrackThreadCore.getInstance().findTo(owner)
                 System.err.println("\t\tThread Replace: "+owner+" == "+descriptor);
             }
             super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
         }
     }
 }
