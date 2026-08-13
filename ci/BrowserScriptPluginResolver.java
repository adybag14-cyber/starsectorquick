package com.fs.starfarer.loading.scripts;

import com.fs.starfarer.api.plugins.LevelupPlugin;
import com.fs.util.container.repo.ObjectRepository;
import data.scripts.plugins.LevelupPluginImpl;
import java.util.List;

/** Browser-only repair for a precompiled core script plugin omitted from ScriptStore's repository. */
public final class BrowserScriptPluginResolver {
    private BrowserScriptPluginResolver() {}

    public static Object resolve(ObjectRepository repository, Class<?> type) {
        List<?> existing = repository.getList(type);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        if (LevelupPlugin.class.getName().equals(type.getName())) {
            repository.add(new LevelupPluginImpl());
            List<?> repaired = repository.getList(type);
            if (!repaired.isEmpty()) {
                System.out.println(
                        "BrowserScriptPluginFix: registered precompiled LevelupPluginImpl for "
                                + type.getName());
                return repaired.get(0);
            }
        }

        // Preserve the stock failure behavior for any unrelated missing plugin.
        return existing.get(0);
    }
}
