import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class ObjectRepositoryRebuildProbe {
    private static final class Marker {
        final int id;
        Marker(final int id) { this.id = id; }
    }

    public static void main(String[] args) throws Exception {
        configureJvmIdentity();
        System.out.println("ObjectRepositoryRebuildProbe: START");
        runCase(1000);
        runCase(5000);
        runCase(10000);
        System.out.println("ObjectRepositoryRebuildProbe: DONE");
    }

    private static void runCase(final int count) throws Exception {
        final Class<?> repoClass = Class.forName("com.fs.util.container.repo.ObjectRepository");
        final Object repo = repoClass.getConstructor().newInstance();
        final Field savedField = repoClass.getDeclaredField("saved");
        savedField.setAccessible(true);
        final List<Object> saved = new ArrayList<Object>(count);
        for (int i = 0; i < count; i++) saved.add(new Marker(i));
        savedField.set(repo, saved);
        final Method readResolve = repoClass.getDeclaredMethod("readResolve");
        readResolve.setAccessible(true);
        final long start = System.currentTimeMillis();
        final Object resolved = readResolve.invoke(repo);
        final long elapsed = System.currentTimeMillis() - start;
        final int size = ((Integer) repoClass.getMethod("size").invoke(resolved)).intValue();
        final List<?> all = (List<?>) repoClass.getMethod("getList", Class.class).invoke(resolved, Object.class);
        System.out.println("ObjectRepositoryRebuildProbe: count=" + count
                + " elapsedMs=" + elapsed + " size=" + size + " objectList=" + all.size());
        if (size != count || all.size() != count) {
            throw new AssertionError("ObjectRepository rebuild mismatch count=" + count
                    + " size=" + size + " objectList=" + all.size());
        }
    }

    private static void configureJvmIdentity() {
        setIfMissing("java.vm.vendor", "CheerpJ");
        setIfMissing("java.vendor", "CheerpJ");
        setIfMissing("java.vm.name", "CheerpJ Runtime");
        setIfMissing("java.specification.version", "1.8");
        setIfMissing("java.specification.vendor", "Oracle Corporation");
        setIfMissing("java.specification.name", "Java Platform API Specification");
    }

    private static void setIfMissing(final String key, final String value) {
        final String current = System.getProperty(key);
        if (current == null || current.trim().length() == 0) System.setProperty(key, value);
    }
}
