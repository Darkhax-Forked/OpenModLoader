package xyz.openmodloader.modloader;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.imageio.ImageIO;

import com.google.common.collect.Sets;
import com.google.gson.annotations.SerializedName;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import xyz.openmodloader.event.strippable.Side;
import xyz.openmodloader.event.strippable.Strippable;

public class ModContainer {
    private transient Class<?> mainClass;
    private transient ResourceLocation logo;
    private transient IMod instance;

    @SerializedName("Mod-Class")
    private String classString;
    @SerializedName("ID")
    private String modID;
    @SerializedName("Name")
    private String name;
    @SerializedName("Major")
    private String major;
    @SerializedName("Minor")
    private String minor;
    @SerializedName("Patch")
    private String patch;
    @SerializedName("Description")
    private String description;
    @SerializedName("UpdateURL")
    private String updateURL;
    @SerializedName("Logo")
    private String logoString;
    @SerializedName("Transformer")
    private String transformer;
    @SerializedName("Dependencies")
    private String dependencies;

    /**
     * Uses a manifest to create a mod container.
     */
    public static ModContainer create(Manifest manifest) {
        Set<Object> attributeNames = manifest.getMainAttributes().keySet();
        if (!attributeNames.containsAll(Arrays.asList(new Attributes.Name("ID"), new Attributes.Name("Mod-Class")))) {
            return null;
        }
        ModContainer container = new ModContainer();
        Attributes attributes = manifest.getMainAttributes();
        for (Field field : ModContainer.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(SerializedName.class)) {
                String name = field.getAnnotation(SerializedName.class).value();
                if (attributeNames.contains(new Attributes.Name(name))) {
                    try {
                        field.set(container, attributes.getValue(name));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }
        }
        return container;
    }

    public Class<?> getMainClass() {
        if (this.mainClass == null) {
            try {
                this.mainClass = Class.forName(this.classString, true, Launch.classLoader);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return mainClass;
    }

    @Strippable(side = Side.CLIENT)
    public ResourceLocation getLogo() {
        if (this.logo == null && this.logoString != null) {
            try {
                InputStream stream = ModContainer.class.getResourceAsStream("/" + this.logoString);
                if (stream == null) {
                    return null;
                }
                BufferedImage image = ImageIO.read(stream);
                DynamicTexture texture = new DynamicTexture(image);
                this.logo = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("mods/" + getModID(), texture);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return logo;
    }

    public IMod getInstance() {
        if (this.instance == null) {
            try {
                this.instance = (IMod) this.getMainClass().newInstance();
            } catch (ClassCastException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    public String getModID() {
        return modID;
    }

    public String getName() {
        return name;
    }

    public String getMajor() {
        return major;
    }

    public String getMinor() {
        return minor;
    }

    public String getPatch() {
        return patch;
    }

    public String getVersion() {
        return getMajor() + "." + getMinor() + "." + getPatch();
    }

    public String getDescription() {
        return description;
    }

    public String getUpdateURL() {
        return updateURL;
    }

    public String getTransformer() {
        return transformer;
    }

    public String[] getDependencies() {
        if (dependencies == null || dependencies.matches("\\s*"))
            return new String[0];
        return dependencies.split("\\s*,\\s*");
    }

    Set<String> getDependencySet() {
        Set<String> set = Sets.newHashSet();
        for (String s: getDependencies())
            set.add(s.split("\\s:\\s")[0]);
        return set;
    }
}
