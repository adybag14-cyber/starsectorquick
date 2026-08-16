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

/** Adds low-volume checkpoints around CampaignGameManager.create() and CampaignFleet(Faction). */
public final class PatchCampaignCreateDiagnostics {
    private static final String CREATE_TARGET =
            "com/fs/starfarer/campaign/save/CampaignGameManager.class";
    private static final String FLEET_TARGET =
            "com/fs/starfarer/campaign/fleet/CampaignFleet.class";
    private static final String FLEET_CTOR_DESC =
            "(Lcom/fs/starfarer/campaign/Faction;)V";
    private static final int EXPECTED_FLEET_CTOR_CALLS = 24;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: PatchCampaignCreateDiagnostics input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] createClassSeen = new int[] {0};
        int[] fleetClassSeen = new int[] {0};
        int[] checkpoints = new int[] {0};
        int[] economyProbes = new int[] {0};
        int[] tutorialHooks = new int[] {0};
        int[] fleetCtorSeen = new int[] {0};
        int[] fleetCtorCalls = new int[] {0};

        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                out.putNextEntry(copy);
                byte[] bytes;
                try (InputStream in = jar.getInputStream(entry)) {
                    bytes = readAll(in);
                }
                if (CREATE_TARGET.equals(entry.getName())) {
                    createClassSeen[0]++;
                    bytes = patchCreate(bytes, checkpoints, economyProbes, tutorialHooks);
                } else if (FLEET_TARGET.equals(entry.getName())) {
                    fleetClassSeen[0]++;
                    bytes = patchFleetConstructor(bytes, fleetCtorSeen, fleetCtorCalls);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (createClassSeen[0] != 1
                || fleetClassSeen[0] != 1
                || checkpoints[0] < 1
                || economyProbes[0] != 1
                || tutorialHooks[0] != 1
                || fleetCtorSeen[0] != 1
                || fleetCtorCalls[0] != EXPECTED_FLEET_CTOR_CALLS) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "Campaign create diagnostics not applied: createClassSeen="
                            + createClassSeen[0]
                            + " fleetClassSeen="
                            + fleetClassSeen[0]
                            + " checkpoints="
                            + checkpoints[0]
                            + " economyProbes="
                            + economyProbes[0]
                            + " tutorialHooks="
                            + tutorialHooks[0]
                            + " fleetCtorSeen="
                            + fleetCtorSeen[0]
                            + " fleetCtorCalls="
                            + fleetCtorCalls[0]
                            + " expectedFleetCtorCalls="
                            + EXPECTED_FLEET_CTOR_CALLS);
        }
        System.out.println(
                "Patched CampaignGameManager create diagnostics checkpoints="
                        + checkpoints[0]
                        + " economyProbes="
                        + economyProbes[0]
                        + " tutorialHooks="
                        + tutorialHooks[0]
                        + " CampaignFleetCtorCalls="
                        + fleetCtorCalls[0]);
    }

    private static byte[] patchCreate(
            byte[] input, int[] checkpoints, int[] economyProbes, int[] tutorialHooks) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor delegate =
                        super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        String phase = classify(owner, methodName, methodDescriptor);
                        if (phase != null) {
                            emit("before " + phase);
                            checkpoints[0]++;
                        }
                        super.visitMethodInsn(
                                opcode, owner, methodName, methodDescriptor, isInterface);
                        if (phase != null) {
                            emit("after " + phase);
                            checkpoints[0]++;
                            if ("Economy.load".equals(phase)) {
                                super.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        "com/fs/starfarer/CampaignInitCompat",
                                        "logEconomyState",
                                        "()V",
                                        false);
                                economyProbes[0]++;
                            }
                            if ("CampaignState.resetViewOffset".equals(phase)) {
                                // This is the stock lifecycle boundary immediately before
                                // CharacterCreationData.getScripts() is executed. The direct
                                // browser path has an empty scripts list, so install the
                                // requested stock tutorial here after the player fleet exists.
                                super.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        "com/fs/starfarer/BrowserTutorialCompat",
                                        "startTutorialIfRequested",
                                        "()V",
                                        false);
                                tutorialHooks[0]++;
                            }
                        }
                    }

                    private void emit(String message) {
                        super.visitFieldInsn(
                                Opcodes.GETSTATIC,
                                "java/lang/System",
                                "out",
                                "Ljava/io/PrintStream;");
                        super.visitLdcInsn("CampaignCreateDiag: " + message);
                        super.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                "java/io/PrintStream",
                                "println",
                                "(Ljava/lang/String;)V",
                                false);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] patchFleetConstructor(
            byte[] input, int[] fleetCtorSeen, int[] fleetCtorCalls) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor delegate =
                        super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"<init>".equals(name) || !FLEET_CTOR_DESC.equals(descriptor)) {
                    return delegate;
                }
                fleetCtorSeen[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    private int ordinal;

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        emit("enter CampaignFleet(Faction)");
                    }

                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        ordinal++;
                        fleetCtorCalls[0]++;
                        String label =
                                "call#"
                                        + ordinal
                                        + " "
                                        + owner
                                        + "."
                                        + methodName
                                        + methodDescriptor;

                        boolean superCtor =
                                ordinal == 1
                                        && opcode == Opcodes.INVOKESPECIAL
                                        && "com/fs/starfarer/campaign/BaseCampaignEntity".equals(owner)
                                        && "<init>".equals(methodName);
                        if (!superCtor) {
                            emit("before " + label);
                        }
                        super.visitMethodInsn(
                                opcode, owner, methodName, methodDescriptor, isInterface);
                        emit("after " + label);
                    }

                    private void emit(String message) {
                        super.visitFieldInsn(
                                Opcodes.GETSTATIC,
                                "java/lang/System",
                                "out",
                                "Ljava/io/PrintStream;");
                        super.visitLdcInsn("CampaignFleetCtorDiag: " + message);
                        super.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                "java/io/PrintStream",
                                "println",
                                "(Ljava/lang/String;)V",
                                false);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static String classify(String owner, String name, String descriptor) {
        if ("com/fs/starfarer/api/campaign/SectorGeneratorPlugin".equals(owner)
                && "generate".equals(name)) {
            return "SectorGeneratorPlugin.generate";
        }
        if ("com/fs/starfarer/api/campaign/SectorProcGenPlugin".equals(owner)) {
            if ("prepare".equals(name)) {
                return "SectorProcGenPlugin.prepare";
            }
            if ("generate".equals(name)) {
                return "SectorProcGenPlugin.generate";
            }
        }
        if ("com/fs/starfarer/api/ModPlugin".equals(owner)) {
            if ("onNewGame".equals(name)
                    || "onNewGameAfterProcGen".equals(name)
                    || "onNewGameAfterEconomyLoad".equals(name)
                    || "onNewGameAfterTimePass".equals(name)
                    || "onGameLoad".equals(name)) {
                return "ModPlugin." + name;
            }
        }
        if ("com/fs/starfarer/campaign/econ/oOOO".equals(owner)
                && "o00000".equals(name)
                && descriptor.startsWith("(Ljava/lang/String;ZZ")) {
            return "Economy.load";
        }
        if ("com/fs/starfarer/campaign/CampaignEngine".equals(owner)
                && "setCurrentLocation".equals(name)) {
            return "CampaignEngine.setCurrentLocation";
        }
        if ("com/fs/starfarer/campaign/CampaignEngine".equals(owner)
                && "setPlayerFleet".equals(name)) {
            return "CampaignEngine.setPlayerFleet";
        }
        if ("com/fs/starfarer/campaign/fleet/CampaignFleet".equals(owner)
                && "<init>".equals(name)
                && "(Lcom/fs/starfarer/campaign/Faction;)V".equals(descriptor)) {
            return "CampaignFleet.<init>";
        }
        if ("com/fs/starfarer/campaign/fleet/FleetMember".equals(owner)
                && "<init>".equals(name)
                && "(ILjava/lang/String;Lcom/fs/starfarer/api/fleet/FleetMemberType;)V".equals(descriptor)) {
            return "FleetMember.<init>";
        }
        if ("com/fs/starfarer/campaign/fleet/FleetData".equals(owner)
                && "syncIfNeeded".equals(name)
                && "()V".equals(descriptor)) {
            return "FleetData.syncIfNeeded";
        }
        if ("com/fs/starfarer/api/campaign/LocationAPI".equals(owner)
                && "spawnFleet".equals(name)) {
            return "LocationAPI.spawnFleet";
        }
        if ("com/fs/starfarer/campaign/fleet/FleetMember".equals(owner)
                && "updateStats".equals(name)
                && "()V".equals(descriptor)) {
            return "FleetMember.updateStats";
        }
        if ("com/fs/starfarer/loading/SpecStore".equals(owner)
                && "Ô00000".equals(name)
                && "(Ljava/lang/Class;)Ljava/util/Collection;".equals(descriptor)) {
            return "SpecStore.getAllSpecs";
        }
        if ("com/fs/starfarer/campaign/CampaignState".equals(owner)
                && "clearMessages".equals(name)
                && "()V".equals(descriptor)) {
            return "CampaignState.clearMessages";
        }
        if ("com/fs/starfarer/campaign/CampaignState".equals(owner)
                && "resetViewOffset".equals(name)
                && "()V".equals(descriptor)) {
            return "CampaignState.resetViewOffset";
        }
        return null;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
