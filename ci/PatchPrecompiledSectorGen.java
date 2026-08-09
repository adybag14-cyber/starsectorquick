import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Patches the SectorGen class that is actually loaded from scripts-precompiled.jar.
 *
 * The repository also contains loose copies of data/scripts/world/SectorGen.java,
 * but the CheerpJ launcher puts scripts-precompiled.jar on the JVM classpath. The
 * precompiled generate() therefore wins before Janino can make the loose source
 * rewrite authoritative.
 *
 * By default all 24 heavyweight vanilla system-generator calls are removed. A
 * third optional argument, or STARSECTOR_RETAIN_WORLD_STEPS, may name specific
 * steps to retain (comma-separated), e.g. "Corvus". This keeps the current
 * skip-all isolation test unchanged while allowing evidence-driven restoration
 * of one real system at a time without another source patch for every experiment.
 *
 * STARSECTOR_MINIMAL_CORVUS_ASHARU_ANCHOR=true is a second, independent diagnostic
 * knob. It keeps all selected system-step behavior above, but before generate()
 * returns it asks BrowserSectorWorldCompat to create only the normal Corvus star
 * and Asharu planet entities. The helper never creates a market; Economy.load()
 * must still build Asharu's stock market from corvus.json.
 */
public final class PatchPrecompiledSectorGen {
    private static final String TARGET_ENTRY = "data/scripts/world/SectorGen.class";
    private static final String TARGET_OWNER = "data/scripts/world/SectorGen";
    private static final String GENERATE_DESC =
            "(Lcom/fs/starfarer/api/campaign/SectorAPI;)V";
    private static final String RUN_STEP_DESC =
            "(Ljava/lang/String;ZLdata/scripts/world/SectorGen$SectorStep;)V";
    private static final String WORLD_COMPAT = "com/fs/starfarer/BrowserSectorWorldCompat";
    private static final List<String> STEP_ORDER = Collections.unmodifiableList(Arrays.asList(
            "Galatia",
            "Askonia",
            "Eos",
            "Valhalla",
            "Arcadia",
            "Magec",
            "Corvus",
            "Aztlan",
            "Samarra",
            "Penelope",
            "Yma",
            "Hybrasil",
            "Duzahk",
            "TiaTaxet",
            "Canaan",
            "AlGebbar",
            "Isirah",
            "KumariKandam",
            "Naraka",
            "Thule",
            "Mayasura",
            "Zagan",
            "Westernesse",
            "Tyle"));
    private static final int EXPECTED_STEPS = STEP_ORDER.size();

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException(
                    "usage: PatchPrecompiledSectorGen input.jar output.jar [retainedStepNames]");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        String retainedRaw =
                args.length == 3 ? args[2] : System.getenv("STARSECTOR_RETAIN_WORLD_STEPS");
        Set<String> retained = parseRetainedSteps(retainedRaw);
        boolean minimalAsharuAnchor =
                Boolean.parseBoolean(System.getenv("STARSECTOR_MINIMAL_CORVUS_ASHARU_ANCHOR"));
        int[] classSeen = new int[] {0};
        int[] generateSeen = new int[] {0};
        int[] encounteredSteps = new int[] {0};
        int[] skippedSteps = new int[] {0};
        int[] retainedSteps = new int[] {0};
        int[] anchorInjections = new int[] {0};

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
                if (TARGET_ENTRY.equals(entry.getName())) {
                    classSeen[0]++;
                    bytes = patch(
                            bytes,
                            retained,
                            minimalAsharuAnchor,
                            generateSeen,
                            encounteredSteps,
                            skippedSteps,
                            retainedSteps,
                            anchorInjections);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        int expectedAnchorInjections = minimalAsharuAnchor ? 1 : 0;
        if (classSeen[0] != 1
                || generateSeen[0] != 1
                || encounteredSteps[0] != EXPECTED_STEPS
                || retainedSteps[0] != retained.size()
                || skippedSteps[0] + retainedSteps[0] != EXPECTED_STEPS
                || anchorInjections[0] != expectedAnchorInjections) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "precompiled SectorGen patch incomplete: classSeen="
                            + classSeen[0]
                            + " generateSeen="
                            + generateSeen[0]
                            + " encounteredSteps="
                            + encounteredSteps[0]
                            + " skippedSteps="
                            + skippedSteps[0]
                            + " retainedSteps="
                            + retainedSteps[0]
                            + " requestedRetained="
                            + retained
                            + " anchorInjections="
                            + anchorInjections[0]
                            + " expectedAnchorInjections="
                            + expectedAnchorInjections
                            + " expectedSteps="
                            + EXPECTED_STEPS);
        }
        System.out.println(
                "Patched scripts-precompiled SectorGen encountered="
                        + encounteredSteps[0]
                        + " skipped="
                        + skippedSteps[0]
                        + " retained="
                        + retained
                        + " minimalAsharuAnchor="
                        + minimalAsharuAnchor);
    }

    private static Set<String> parseRetainedSteps(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> retained = new LinkedHashSet<String>();
        for (String token : raw.split(",")) {
            String requested = token == null ? "" : token.trim();
            if (requested.isEmpty()) {
                continue;
            }
            String canonical = null;
            for (String known : STEP_ORDER) {
                if (known.toLowerCase(Locale.ROOT).equals(requested.toLowerCase(Locale.ROOT))) {
                    canonical = known;
                    break;
                }
            }
            if (canonical == null) {
                throw new IllegalArgumentException(
                        "unknown retained SectorGen step '" + requested + "'; known=" + STEP_ORDER);
            }
            retained.add(canonical);
        }
        return retained;
    }

    private static byte[] patch(
            byte[] input,
            Set<String> retained,
            boolean minimalAsharuAnchor,
            int[] generateSeen,
            int[] encounteredSteps,
            int[] skippedSteps,
            int[] retainedSteps,
            int[] anchorInjections) {
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
                if (!"generate".equals(name) || !GENERATE_DESC.equals(descriptor)) {
                    return delegate;
                }
                generateSeen[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        super.visitFieldInsn(
                                Opcodes.GETSTATIC,
                                "java/lang/System",
                                "out",
                                "Ljava/io/PrintStream;");
                        super.visitLdcInsn(
                                "BrowserSectorGenDiag: executing patched scripts-precompiled SectorGen.generate retained="
                                        + retained
                                        + " minimalAsharuAnchor="
                                        + minimalAsharuAnchor);
                        super.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                "java/io/PrintStream",
                                "println",
                                "(Ljava/lang/String;)V",
                                false);
                    }

                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && TARGET_OWNER.equals(owner)
                                && "runSectorStep".equals(methodName)
                                && RUN_STEP_DESC.equals(methodDescriptor)) {
                            int ordinal = encounteredSteps[0]++;
                            if (ordinal >= STEP_ORDER.size()) {
                                throw new IllegalStateException(
                                        "encountered more SectorGen runSectorStep calls than expected");
                            }
                            String stepName = STEP_ORDER.get(ordinal);
                            if (retained.contains(stepName)) {
                                retainedSteps[0]++;
                                super.visitMethodInsn(
                                        opcode, owner, methodName, methodDescriptor, isInterface);
                                return;
                            }

                            // Operand stack is [this, label, compatibilityFastPath, step].
                            // All four operands are category-1 values; two POP2s consume
                            // exactly the arguments and receiver that the removed call used.
                            super.visitInsn(Opcodes.POP2);
                            super.visitInsn(Opcodes.POP2);
                            skippedSteps[0]++;
                            return;
                        }
                        super.visitMethodInsn(
                                opcode, owner, methodName, methodDescriptor, isInterface);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN && minimalAsharuAnchor) {
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    WORLD_COMPAT,
                                    "ensureMinimalCorvusAsharuAnchor",
                                    "()V",
                                    false);
                            anchorInjections[0]++;
                        }
                        super.visitInsn(opcode);
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
