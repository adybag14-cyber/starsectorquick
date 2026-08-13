import com.fs.starfarer.api.plugins.LevelupPlugin;
import com.fs.starfarer.loading.scripts.BrowserScriptPluginResolver;
import com.fs.util.container.repo.ObjectRepository;

public final class VerifyScriptPluginFallback {
    private VerifyScriptPluginFallback() {}

    public static void main(String[] args) {
        ObjectRepository repository = new ObjectRepository();
        Object first = BrowserScriptPluginResolver.resolve(repository, LevelupPlugin.class);
        Object second = BrowserScriptPluginResolver.resolve(repository, LevelupPlugin.class);
        if (!(first instanceof LevelupPlugin)) {
            throw new AssertionError("fallback did not return a LevelupPlugin: " + first);
        }
        if (first != second) {
            throw new AssertionError("fallback did not reuse the registered plugin instance");
        }
        if (repository.getList(LevelupPlugin.class).size() != 1) {
            throw new AssertionError("expected exactly one registered LevelupPlugin");
        }
        System.out.println("VerifyScriptPluginFallback: OK " + first.getClass().getName());
    }
}
