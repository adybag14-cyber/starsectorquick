import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Adds timing-only probes to CampaignGameManager's stock new-game method. */
public final class PatchCampaignCreatePerfDiagnostics {
    private static final String TARGET = "com/fs/starfarer/campaign/save/CampaignGameManager.class";
    private static final String METHOD = "o00000";
    private static final String DESC_SUFFIX =
            ";Lcom/fs/starfarer/campaign/CampaignState;)Ljava/lang/String;";
    private static final String DIAG = "com/fs/starfarer/BrowserCampaignCreatePerfDiag";

    private PatchCampaignCreatePerfDiagnostics() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchCampaignCreatePerfDiagnostics input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Shape shape = new Shape();
        int[] classes = {0};
        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                out.putNextEntry(copy);
                byte[] bytes;
                try (InputStream in = jar.getInputStream(entry)) { bytes = readAll(in); }
                if (TARGET.equals(entry.getName())) {
                    classes[0]++;
                    bytes = patch(bytes, shape);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || !shape.valid()) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("Campaign create perf diagnostic shape mismatch classes="
                    + classes[0] + " " + shape);
        }
        System.out.println("Patched CampaignGameManager new-game performance diagnostics " + shape);
    }

    private static byte[] patch(byte[] input, Shape shape) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                boolean target = METHOD.equals(name)
                        && descriptor.startsWith("(Lcom/fs/starfarer/campaign/save/")
                        && descriptor.endsWith(DESC_SUFFIX);
                if (!target) return mv;
                shape.methods++;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    private int fleetCtorOrdinal;

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        call("begin", "()V");
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKEINTERFACE
                                && "com/fs/starfarer/api/campaign/SectorProcGenPlugin".equals(owner)
                                && "prepare".equals(methodName)) {
                            shape.prepareCalls++;
                            aroundMark(opcode, owner, methodName, methodDescriptor, isInterface,
                                    "procgenPrepare");
                            return;
                        }
                        if (opcode == Opcodes.INVOKEINTERFACE
                                && "com/fs/starfarer/api/campaign/SectorGeneratorPlugin".equals(owner)
                                && "generate".equals(methodName)) {
                            shape.sectorGeneratorCalls++;
                            call("beforeSectorGenerator", "()V");
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            call("afterSectorGenerator", "()V");
                            return;
                        }
                        if ((opcode == Opcodes.INVOKEINTERFACE
                                && "com/fs/starfarer/api/campaign/SectorProcGenPlugin".equals(owner)
                                && "generate".equals(methodName))
                                || (opcode == Opcodes.INVOKESTATIC
                                && "com/fs/starfarer/CampaignInitCompat".equals(owner)
                                && "generateSectorProcGen".equals(methodName))) {
                            shape.procgenCalls++;
                            aroundMark(opcode, owner, methodName, methodDescriptor, isInterface,
                                    "outerProcgen");
                            return;
                        }
                        if (opcode == Opcodes.INVOKESTATIC
                                && "com/fs/starfarer/campaign/econ/oOOO".equals(owner)
                                && "o00000".equals(methodName)
                                && methodDescriptor.startsWith("(Ljava/lang/String;ZZ")) {
                            shape.economyLoadCalls++;
                            aroundMark(opcode, owner, methodName, methodDescriptor, isInterface,
                                    "economyLoad");
                            return;
                        }
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && "com/fs/starfarer/campaign/CampaignEngine".equals(owner)
                                && "advance".equals(methodName)
                                && "(FLcom/fs/starfarer/util/super/B;)V".equals(methodDescriptor)) {
                            shape.advanceCallsites++;
                            call("beforeAdvance", "()V");
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            call("afterAdvance", "()V");
                            return;
                        }
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && "com/fs/starfarer/campaign/econ/reach/ReachEconomyStepper".equals(owner)
                                && "nextFrame".equals(methodName)
                                && "(F)V".equals(methodDescriptor)) {
                            shape.economyStepCallsites++;
                            call("beforeEconomyStep", "()V");
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            call("afterEconomyStep", "()V");
                            return;
                        }
                        if (opcode == Opcodes.INVOKESPECIAL
                                && "com/fs/starfarer/campaign/fleet/CampaignFleet".equals(owner)
                                && "<init>".equals(methodName)
                                && "(Lcom/fs/starfarer/campaign/Faction;)V".equals(methodDescriptor)) {
                            fleetCtorOrdinal++;
                            shape.fleetCtorCalls++;
                            aroundMark(opcode, owner, methodName, methodDescriptor, isInterface,
                                    fleetCtorOrdinal == 1 ? "preTimePassFleet" : "playerFleet");
                            return;
                        }
                        if (opcode == Opcodes.INVOKESTATIC
                                && "com/fs/starfarer/loading/SpecStore".equals(owner)
                                && "Ô00000".equals(methodName)
                                && "(Ljava/lang/Class;)Ljava/util/Collection;".equals(methodDescriptor)) {
                            shape.abilitySpecCalls++;
                            aroundMark(opcode, owner, methodName, methodDescriptor, isInterface,
                                    "abilitySpecCollection");
                            return;
                        }
                        if (opcode == Opcodes.INVOKEINTERFACE
                                && "com/fs/starfarer/api/ModPlugin".equals(owner)) {
                            String phase = null;
                            if ("onNewGame".equals(methodName)) phase = "onNewGame";
                            else if ("onNewGameAfterProcGen".equals(methodName)) phase = "onNewGameAfterProcGen";
                            else if ("onNewGameAfterEconomyLoad".equals(methodName)) phase = "onNewGameAfterEconomyLoad";
                            else if ("onNewGameAfterTimePass".equals(methodName)) phase = "onNewGameAfterTimePass";
                            else if ("onGameLoad".equals(methodName)) phase = "onGameLoad";
                            if (phase != null) {
                                shape.modHookCallsites++;
                                aroundMark(opcode, owner, methodName, methodDescriptor, isInterface, phase);
                                return;
                            }
                        }
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && "com/fs/starfarer/campaign/CampaignState".equals(owner)
                                && "clearMessages".equals(methodName)) {
                            shape.finalUiCalls++;
                            mark("finalUi-before");
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && "com/fs/starfarer/campaign/CampaignState".equals(owner)
                                && "resetViewOffset".equals(methodName)) {
                            mark("finalUi-after");
                        }
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.ARETURN || opcode == Opcodes.ATHROW) {
                            call("finish", "()V");
                        }
                        super.visitInsn(opcode);
                    }

                    private void aroundMark(int opcode, String owner, String methodName,
                                            String methodDescriptor, boolean isInterface, String phase) {
                        mark(phase + "-before");
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                        mark(phase + "-after");
                    }

                    private void mark(String label) {
                        super.visitLdcInsn(label);
                        call("mark", "(Ljava/lang/String;)V");
                    }

                    private void call(String name, String desc) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, DIAG, name, desc, false);
                    }
                };
            }
        }, 0);
        return writer.toByteArray();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }

    private static final class Shape {
        int methods;
        int prepareCalls;
        int sectorGeneratorCalls;
        int procgenCalls;
        int economyLoadCalls;
        int advanceCallsites;
        int economyStepCallsites;
        int fleetCtorCalls;
        int abilitySpecCalls;
        int modHookCallsites;
        int finalUiCalls;
        boolean valid() {
            return methods == 1 && prepareCalls == 1 && sectorGeneratorCalls == 2
                    && procgenCalls == 1 && economyLoadCalls == 1
                    && advanceCallsites == 2 && economyStepCallsites == 2
                    && fleetCtorCalls == 2 && abilitySpecCalls == 1
                    && modHookCallsites == 5 && finalUiCalls == 1;
        }
        public String toString() {
            return "methods=" + methods + " prepare=" + prepareCalls
                    + " sectorGenerators=" + sectorGeneratorCalls + " procgen=" + procgenCalls
                    + " economyLoad=" + economyLoadCalls + " advanceCallsites=" + advanceCallsites
                    + " economyStepCallsites=" + economyStepCallsites + " fleetCtors=" + fleetCtorCalls
                    + " abilitySpecs=" + abilitySpecCalls + " modHooks=" + modHookCallsites
                    + " finalUi=" + finalUiCalls;
        }
    }
}
