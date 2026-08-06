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

/** Adds low-volume checkpoints around the expensive phases of CampaignGameManager.create(). */
public final class PatchCampaignCreateDiagnostics {
    private static final String TARGET =
            "com/fs/starfarer/campaign/save/CampaignGameManager.class";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: PatchCampaignCreateDiagnostics input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classSeen = new int[] {0};
        int[] checkpoints = new int[] {0};
        int[] economyProbes = new int[] {0};

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
                if (TARGET.equals(entry.getName())) {
                    classSeen[0]++;
                    bytes = patch(bytes, checkpoints, economyProbes);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (classSeen[0] != 1 || checkpoints[0] < 1 || economyProbes[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "CampaignGameManager diagnostics not applied: classSeen="
                            + classSeen[0]
                            + " checkpoints="
                            + checkpoints[0]
                            + " economyProbes="
                            + economyProbes[0]);
        }
        System.out.println(
                "Patched CampaignGameManager create diagnostics checkpoints="
                        + checkpoints[0]
                        + " economyProbes="
                        + economyProbes[0]);
    }

    private static byte[] patch(byte[] input, int[] checkpoints, int[] economyProbes) {
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

        // The first half of create() is already well covered above. These exact
        // calls cover the previously opaque tail that constructs the real player
        // fleet, resolves the starting variant, spawns it into hyperspace, syncs
        // fleet data, and finally initializes CampaignState UI state.
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
