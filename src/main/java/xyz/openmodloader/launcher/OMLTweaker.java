package xyz.openmodloader.launcher;

import java.io.File;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class OMLTweaker implements ITweaker {

	@Override
	public void acceptOptions (List<String> args, File gameDir, File assetsDir, String profile) {

	}

	@Override
	public void injectIntoClassLoader (LaunchClassLoader classLoader) {

	}

	@Override
	public String getLaunchTarget () {
		return null;
	}

	@Override
	public String[] getLaunchArguments () {
		return new String[0];
	}
}