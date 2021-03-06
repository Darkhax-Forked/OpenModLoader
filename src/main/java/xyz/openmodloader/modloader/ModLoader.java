package xyz.openmodloader.modloader;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Multimap;

import net.minecraft.launchwrapper.Launch;
import xyz.openmodloader.OpenModLoader;
import xyz.openmodloader.launcher.OMLAccessTransformer;

public class ModLoader {
    /**
     * A list of all loaded mods.
     */
    public static final List<ModContainer> MODS = new ArrayList<>();

    /**
     * A map of all loaded mods. Key is the mod class and value is the ModContainer.
     */
    private static final Map<IMod, ModContainer> MODS_MAP = new HashMap<>();

    /**
     * A map of all loaded mods. Key is the mod id and value is the ModContainer.
     */
    private static final Map<String, ModContainer> ID_MAP = new HashMap<>();

    /**
     * The running directory for the game.
     */
    private static final File RUN_DIRECTORY = new File(".");

    /**
     * The directory to load mods from.
     */
    private static final File MOD_DIRECTORY = new File(RUN_DIRECTORY, "mods");

    /**
     * Attempts to load all mods from the mods directory. While this is public,
     * it is intended for internal use only!
     */
    public static void loadMods() {
        try {
            List<ModContainer> unsortedMods = new ArrayList<>();
            if (MOD_DIRECTORY.exists()) {
                File[] files = MOD_DIRECTORY.listFiles();
                if (files != null) {
                    for (File mod : files) {
                        Launch.classLoader.addURL(mod.toURI().toURL());
                    }
                }
            }

            URL roots;
            Enumeration<URL> metas = Launch.classLoader.getResources("META-INF");
            while (metas.hasMoreElements()) {
                roots = metas.nextElement();
                File root = new File(roots.getPath());
                File[] files = root.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().equals("MANIFEST.MF")) {
                            FileInputStream stream = new FileInputStream(file);
                            ModContainer mod = loadMod(file, new Manifest(stream));
                            if (mod != null) {
                                unsortedMods.add(mod);
                                ID_MAP.put(mod.getModID(), mod);
                            }
                            stream.close();
                        } else if (file.getName().endsWith(".at")) {
                            Multimap<String, String> entries = OMLAccessTransformer.getEntries();
                            FileUtils.readLines(file).stream().filter(line -> line.matches("\\w+((\\.\\w+)+|)\\s+(\\w+(\\(\\S+|)|\\*\\(\\)|\\*)")).forEach(line -> {
                                String[] parts = line.split(" ");
                                entries.put(parts[0], parts[1]);
                            });
                        }
                    }
                }
            }
            MODS.addAll(DependencySorter.sort(unsortedMods));
            for (ModContainer mod: MODS)
                for (String dep: mod.getDependencies()) {
                    String[] depParts = dep.split("\\s*:\\s*");
                    ModContainer depContainer = ID_MAP.get(depParts[0]);
                    if (depContainer == null)
                        throw new RuntimeException("Missing dependency '" + dep + "' for mod '" + mod.getName() + "'.");
                    else if (depParts.length > 1 && !new Version(depContainer.getVersion()).atLeast(new Version(depParts[1])))
                        throw new RuntimeException("Outdated dependency '" + dep + "' for mod '" + mod.getName() + "'. Expected version '" + depParts[1] + "', but got version '" + depContainer.getVersion() + "'.");
                }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Attempts to load a mod from an input stream. This will parse the
     * mods.json file.
     *
     * @param manifest the manifest instance
     */
    private static ModContainer loadMod(File file, Manifest manifest) {
        ModContainer container = ModContainer.create(manifest);
        if (container == null) {
            OpenModLoader.INSTANCE.getLogger().error("Found invalid manifest in file " + file.getAbsolutePath().replace("!", "").replace(File.separator + "META-INF" + File.separator + "MANIFEST.MF", ""));
            return null;
        }
        OpenModLoader.INSTANCE.getLogger().info("Found mod " + container.getName() + " with id " + container.getModID());
        if (container.getTransformer() != null) {
            Launch.classLoader.registerTransformer(container.getTransformer());
        }
        return container;
    }

    /**
     * Iterates through all registered mods and enables them. If there is an
     * issue in registering the mod, it will be disabled.
     */
    public static void registerMods() {
        for (ModContainer mod : MODS) {
            MODS_MAP.put(mod.getInstance(), mod); // load the instances
        }
        for (ModContainer mod : MODS) {
            try {
                mod.getInstance().onEnable();
            } catch (RuntimeException e) {
                OpenModLoader.INSTANCE.getLogger().warn("An error occurred while enabling mod " + mod.getModID());
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Get the mod container of a mod.
     *
     * @param mod the mod instance
     * @return the mod container
     */
    public static ModContainer getContainer(IMod mod) {
        return MODS_MAP.get(mod);
    }

    /**
     * Get the mod container of a mod.
     *
     * @param mod the mod id
     * @return the mod container
     */
    public static ModContainer getContainer(String id) {
        return ID_MAP.get(id);
    }
}
