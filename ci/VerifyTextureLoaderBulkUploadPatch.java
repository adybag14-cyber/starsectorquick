import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Structurally proves the TextureLoader stock pixel loop and required side effects. */
public final class VerifyTextureLoaderBulkUploadPatch {
    private static final String TARGET_ENTRY = "com/fs/graphics/TextureLoader.class";
    private static final String TARGET = "com/fs/graphics/TextureLoader";
    private static final String DESC = "(Ljava/awt/image/BufferedImage;Lcom/fs/graphics/Object;)Ljava/nio/ByteBuffer;";
    private static final String COMPAT = "com/fs/starfarer/TextureUploadCompat";
    private static final String PREPARED = "com/fs/starfarer/TextureUploadCompat$PreparedTexture";
    private static final String OBJECT = "com/fs/graphics/Object";
    private static final String AVERAGE_FIELD = "\u00f500000";
    private static final String MEDIAN_FIELD = "interface";
    private static final String ACCENT_FIELD = "\u00d300000";
    private static final String REUSE_FIELD = "oO0000";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyTextureLoaderBulkUploadPatch patched.jar");
        int[] methods={0}, prepareCalls={0}, selectorCalls={0}, colorPuts={0}, bufferGets={0};
        int[] widthGets={0}, heightGets={0}, dimensionSetters={0}, reuseGets={0}, reusePuts={0};
        int[] rasterPixelCalls={0}, indexedBufferPuts={0}, instructions={0}, controlFlow={0};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            JarEntry entry = jar.getJarEntry(TARGET_ENTRY);
            if (entry == null) throw new AssertionError("missing " + TARGET_ENTRY);
            try (InputStream in = jar.getInputStream(entry)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                        if (!"super".equals(name) || !DESC.equals(descriptor)) return null;
                        methods[0]++;
                        return new MethodVisitor(Opcodes.ASM9) {
                            private void insn(){ instructions[0]++; }
                            @Override public void visitInsn(int opcode){insn();}
                            @Override public void visitIntInsn(int opcode,int operand){insn();}
                            @Override public void visitVarInsn(int opcode,int var){insn();}
                            @Override public void visitTypeInsn(int opcode,String type){insn();}
                            @Override public void visitFieldInsn(int opcode,String owner,String name,String descriptor){
                                insn();
                                if(opcode==Opcodes.PUTFIELD && TARGET.equals(owner) && "Ljava/awt/Color;".equals(descriptor)
                                        && (AVERAGE_FIELD.equals(name)||MEDIAN_FIELD.equals(name)||ACCENT_FIELD.equals(name))) colorPuts[0]++;
                                if(TARGET.equals(owner)&&REUSE_FIELD.equals(name)&&"Ljava/nio/ByteBuffer;".equals(descriptor)){
                                    if(opcode==Opcodes.GETSTATIC) reuseGets[0]++;
                                    if(opcode==Opcodes.PUTSTATIC) reusePuts[0]++;
                                }
                            }
                            @Override public void visitMethodInsn(int opcode,String owner,String methodName,String methodDescriptor,boolean isInterface){
                                insn();
                                if(opcode==Opcodes.INVOKESTATIC && COMPAT.equals(owner) && "prepareTexture".equals(methodName)) prepareCalls[0]++;
                                if(opcode==Opcodes.INVOKESTATIC && COMPAT.equals(owner) && "selectReusable2048Rgb".equals(methodName)) selectorCalls[0]++;
                                if(opcode==Opcodes.INVOKEVIRTUAL && PREPARED.equals(owner) && "getBuffer".equals(methodName)) bufferGets[0]++;
                                if(opcode==Opcodes.INVOKEVIRTUAL && PREPARED.equals(owner) && "getPaddedWidth".equals(methodName)) widthGets[0]++;
                                if(opcode==Opcodes.INVOKEVIRTUAL && PREPARED.equals(owner) && "getPaddedHeight".equals(methodName)) heightGets[0]++;
                                if(opcode==Opcodes.INVOKEVIRTUAL && OBJECT.equals(owner) && "(I)V".equals(methodDescriptor)
                                        && ("Object".equals(methodName)||"\u00d400000".equals(methodName))) dimensionSetters[0]++;
                                if(owner.endsWith("Raster") && "getPixel".equals(methodName)) rasterPixelCalls[0]++;
                                if("java/nio/ByteBuffer".equals(owner)&&"put".equals(methodName)&&"(IB)Ljava/nio/ByteBuffer;".equals(methodDescriptor)) indexedBufferPuts[0]++;
                            }
                            @Override public void visitInvokeDynamicInsn(String n,String d,Handle b,Object... a){insn();}
                            @Override public void visitJumpInsn(int opcode,Label label){insn();controlFlow[0]++;}
                            @Override public void visitLdcInsn(Object value){insn();}
                            @Override public void visitIincInsn(int var,int increment){insn();}
                            @Override public void visitTableSwitchInsn(int min,int max,Label dflt,Label... labels){insn();controlFlow[0]++;}
                            @Override public void visitLookupSwitchInsn(Label dflt,int[] keys,Label[] labels){insn();controlFlow[0]++;}
                            @Override public void visitMultiANewArrayInsn(String descriptor,int dims){insn();}
                        };
                    }
                },0);
            }
        }
        boolean ok=methods[0]==1&&prepareCalls[0]==1&&selectorCalls[0]==1&&colorPuts[0]==3&&bufferGets[0]==1
                &&widthGets[0]==1&&heightGets[0]==1&&dimensionSetters[0]==2&&reuseGets[0]==2&&reusePuts[0]==1
                &&rasterPixelCalls[0]==0&&indexedBufferPuts[0]==0&&controlFlow[0]==0&&instructions[0]<=48;
        if(!ok) throw new AssertionError("texture bulk patch mismatch methods="+methods[0]+" prepareCalls="+prepareCalls[0]
                +" selectorCalls="+selectorCalls[0]+" colorPuts="+colorPuts[0]+" bufferGets="+bufferGets[0]
                +" widthGets="+widthGets[0]+" heightGets="+heightGets[0]+" dimensionSetters="+dimensionSetters[0]
                +" reuseGets="+reuseGets[0]+" reusePuts="+reusePuts[0]+" rasterPixelCalls="+rasterPixelCalls[0]
                +" indexedBufferPuts="+indexedBufferPuts[0]+" controlFlow="+controlFlow[0]+" instructions="+instructions[0]);
        System.out.println("VerifyTextureLoaderBulkUploadPatch: OK instructions="+instructions[0]+" prepareCalls="+prepareCalls[0]
                +" dimensions="+dimensionSetters[0]+" colors="+colorPuts[0]+" reuse="+reuseGets[0]+"/"+reusePuts[0]);
    }
}
