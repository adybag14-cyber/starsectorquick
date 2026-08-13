package com.fs.starfarer.loading;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Verifies manifest variant discovery matches the core filesystem exactly. */
public final class VerifyBrowserVariantIndex {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyBrowserVariantIndex content-root");
        File root = new File(args[0]).getCanonicalFile();
        File variants = new File(root, "data/variants");
        if (!variants.isDirectory()) throw new AssertionError("variant root missing: " + variants);

        List<String> directories = BrowserVariantIndex.readDirectories(root, "data/variants");
        List<String> indexed = new ArrayList<String>();
        indexed.addAll(BrowserVariantIndex.readVariantFiles(root, "data/variants", "variant"));
        for (String directory : directories) {
            indexed.addAll(BrowserVariantIndex.readVariantFiles(root, directory, "variant"));
        }

        Set<String> indexedSet = new HashSet<String>(indexed);
        if (indexedSet.size() != indexed.size()) throw new AssertionError("duplicate manifest variants");

        // Match the stock browser scope: root files plus only child directories
        // advertised by the root index. Physical directories omitted from index.list
        // (for example generated codex_autogen assets) are intentionally invisible
        // to the HTTP resource manager and must not be added by this fast path.
        List<String> actual = new ArrayList<String>();
        collectVariants(new File(variants, "."), "data/variants", actual);
        for (String directory : directories) {
            String child = directory.substring("data/variants/".length());
            collectVariants(new File(variants, child), directory, actual);
        }
        Set<String> actualSet = new HashSet<String>(actual);
        if (!indexedSet.equals(actualSet)) {
            Set<String> missing = new HashSet<String>(actualSet);
            missing.removeAll(indexedSet);
            Set<String> extra = new HashSet<String>(indexedSet);
            extra.removeAll(actualSet);
            throw new AssertionError("variant manifest mismatch missing=" + missing + " extra=" + extra);
        }

        List<String> sortedDirectories = new ArrayList<String>(directories);
        Collections.sort(sortedDirectories);
        if (directories.size() < 1 || indexed.size() < 1) throw new AssertionError("empty variant manifest");
        System.out.println("VerifyBrowserVariantIndex: OK directories=" + directories.size()
                + " variants=" + indexed.size());
    }
    private static void collectVariants(File directory, String logicalDirectory, List<String> out) {
        File[] files = directory.listFiles();
        if (files == null) throw new AssertionError("unable to list indexed variant directory: " + directory);
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".variant")) {
                out.add(logicalDirectory + "/" + file.getName());
            }
        }
    }

}
