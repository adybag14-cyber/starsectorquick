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
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Makes slipstream visual/proximity advancement tolerate the short browser
 * transition window where Campaign State exists before its viewport is attached.
 *
 * The stock implementation immediately dereferences SectorAPI.getViewport() in
 * advanceNearbySegments(float) and addParticles(). During the direct CheerpJ
 * Title -> Campaign transition that value can temporarily be null, turning an
 * otherwise successful new-game bootstrap into a fatal NPE on the first tick.
 * Returning for that tick is safe: both routines are per-frame slipstream visual
 * and nearby-segment maintenance and run again once the viewport is available.
 */
public final class PatchSlipstreamBrowserAdvance {
    private static final String TARGET =
            "com/fs/starfarer/api/impl/campaign/velfield/SlipstreamTerrainPlugin2.class";
    private static final String CLASS =
            "com/fs/starfarer/api/impl/campaign/velfield/SlipstreamTerrainPlugin2";
    private static final String GLOBAL = "com/fs/starfarer/api/Global";
    private static final String SECTOR_API = "com/fs/starfarer/api/campaign/SectorAPI";
    private static final String MISC = "com/fs/starfarer/api/util/Misc";
    private static final String GET_SECTOR_DESC =
            "()Lcom/fs/starfarer/api/campaign/SectorAPI;";
    private static final String GET_VIEWPORT_DESC =
            "()Lcom/fs/starfarer/api/combat/ViewportAPI;";
    private static final String GET_HYPERSPACE_TERRAIN_DESC =
            "()Lcom/fs/starfarer/api/campaign/CampaignTerrainAPI;";

    private PatchSlipstreamBrowserAdvance() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: PatchSlipstreamBrowserAdvance input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] viewportGuards = new int[] {0};
        int[] hyperspaceTerrainGuards = new int[] {0};
        boolean[] targetSeen = new boolean[] {false};

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
                    targetSeen[0] = true;
                    bytes = patch(bytes, viewportGuards, hyperspaceTerrainGuards);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (!targetSeen[0]) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("missing runtime target " + TARGET);
        }
        if (viewportGuards[0] != 2) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "expected viewport guards in exactly two slipstream methods, found "
                            + viewportGuards[0]);
        }
        if (hyperspaceTerrainGuards[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "expected one hyperspace-terrain guard, found "
                            + hyperspaceTerrainGuards[0]);
        }
        System.out.println(
                "Patched SlipstreamTerrainPlugin2 browser first-frame guards viewport="
                        + viewportGuards[0]
                        + " hyperspaceTerrain="
                        + hyperspaceTerrainGuards[0]);
    }

    private static byte[] patch(
            byte[] input, int[] viewportGuards, int[] hyperspaceTerrainGuards) {
        ClassReader reader = new ClassReader(input);
        if (!CLASS.equals(reader.getClassName())) {
            throw new IllegalArgumentException("unexpected class " + reader.getClassName());
        }
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(
                        access, name, descriptor, signature, exceptions);
                boolean nearby = "advanceNearbySegments".equals(name)
                        && "(F)V".equals(descriptor);
                boolean particles = "addParticles".equals(name)
                        && "()V".equals(descriptor);
                if (!nearby && !particles) {
                    return delegate;
                }
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        Label viewportReady = new Label();
                        super.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                GLOBAL,
                                "getSector",
                                GET_SECTOR_DESC,
                                false);
                        super.visitMethodInsn(
                                Opcodes.INVOKEINTERFACE,
                                SECTOR_API,
                                "getViewport",
                                GET_VIEWPORT_DESC,
                                true);
                        super.visitJumpInsn(Opcodes.IFNONNULL, viewportReady);
                        super.visitInsn(Opcodes.RETURN);
                        super.visitLabel(viewportReady);
                        super.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        viewportGuards[0]++;

                        if (nearby) {
                            Label hyperspaceTerrainReady = new Label();
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    MISC,
                                    "getHyperspaceTerrain",
                                    GET_HYPERSPACE_TERRAIN_DESC,
                                    false);
                            super.visitJumpInsn(
                                    Opcodes.IFNONNULL, hyperspaceTerrainReady);
                            super.visitInsn(Opcodes.RETURN);
                            super.visitLabel(hyperspaceTerrainReady);
                            super.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                            hyperspaceTerrainGuards[0]++;
                        }
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
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
