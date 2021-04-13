package org.asf.cyan;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.function.Consumer;

import org.asf.connective.usermanager.api.AuthResult;
import org.asf.connective.usermanager.api.ServiceManager;
import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.webserver.commands.trust.services.LoginService;
import org.asf.cyan.webserver.commands.trust.services.SessionManager;
import org.asf.cyan.webserver.config.virtual.ModInfo;
import org.asf.rats.Memory;
import org.asf.rats.ModuleBasedConfiguration;

@CYAN_COMPONENT
public class CyanTrustServerModule extends CyanTrustServerModificationManager {

	private static HashMap<String, String> configuration = new HashMap<String, String>();
	private static boolean hasConfigChanged = false;

	@Override
	protected String moduleId() {
		return "CyanTrustServer";
	}

	static {
		configuration.put("mod-info-dir", "cyan-modinfo");
		configuration.put("mod-development-group", "moddev");
		configuration.put("mod-security-path", "/cyan/security");
		configuration.put("mod-trust-repository-path", "/cyan/trust");
		
		configuration.put("max-mod-groups-per-user", "5");
		configuration.put("max-mods-per-group", "10");
	}
	
	public static int getMaxModGroups() {
		return Integer.valueOf(configuration.get("max-mod-groups-per-user"));
	}
	
	public static int getMaxMods() {
		return Integer.valueOf(configuration.get("max-mods-per-group"));
	}

	public static File getModInfoDir() {
		return new File(configuration.get("mod-info-dir"));
	}

	public static String getSecurityPath() {
		String path = configuration.get("mod-security-path").replace("\\", "/");
		while (path.contains("//"))
			path = path.replace("//", "/");
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		while (path.startsWith("/"))
			path = path.substring(1);
		return "/" + path;
	}

	public static String getTrustRepositoryPath() {
		String path = configuration.get("mod-trust-repository-path").replace("\\", "/");
		while (path.contains("//"))
			path = path.replace("//", "/");
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		while (path.startsWith("/"))
			path = path.substring(1);
		return "/" + path;
	}

	protected static void initComponent() {
		assign(new CyanTrustServerModule());
		CyanTrustServerModule.start();
	}

	@Override
	protected void startModule() {
		Memory.getInstance().getOrCreate("bootstrap.call").<Runnable>append(() -> {
			Memory.getInstance().getOrCreate("users.delete").<Consumer<AuthResult>>append((user) -> {
				if (user.getGroup().equals(getDevGroup())) {
					SessionManager.deleteSession(user);
					String[] groups = user.getUserStorage().get("mod-groups");
					if (groups != null) {
						for (String modgroup : groups) {
							File moddir = new File(CyanTrustServerModule.getModInfoDir(), modgroup);
							if (moddir.exists())
								deleteDir(moddir);
						}
					}
				}
			});
			readConfig();
		});
		Memory.getInstance().getOrCreate("bootstrap.reload").<Runnable>append(() -> readConfig());
	}

	public static void deleteDir(File input) {
		for (File dir : input.listFiles((t) -> t.isDirectory())) {
			deleteDir(dir);
			dir.delete();
		}
		for (File file : input.listFiles((t) -> !t.isDirectory()))
			file.delete();

		input.delete();
	}

	private void readConfig() {
		try {
			Class.forName("org.asf.connective.usermanager.UserManagerModule", false, getClass().getClassLoader());
		} catch (Exception e) {
			fatal("The UserManager module has not been installed, cannot continue!");
			System.exit(-1);
		}

		hasConfigChanged = false;
		ModuleBasedConfiguration<?> config = Memory.getInstance().get("memory.modules.shared.config")
				.getValue(ModuleBasedConfiguration.class);

		HashMap<String, String> category = config.modules.getOrDefault(moduleId(), new HashMap<String, String>());

		if (!config.modules.containsKey(moduleId())) {
			category.putAll(configuration);
			hasConfigChanged = true;

		} else {
			configuration.forEach((key, value) -> {
				if (!category.containsKey(key)) {
					hasConfigChanged = true;
					category.put(key, value);
				} else {
					configuration.put(key, category.get(key));
				}

			});
		}

		config.modules.put(moduleId(), category);

		LoginService service = new LoginService();
		ServiceManager.registerService(service);

		if (!CyanTrustServerModule.getModInfoDir().exists()) {
			CyanTrustServerModule.getModInfoDir().mkdirs();
			ServiceManager.addToConfiguration(service);
		}

		if (hasConfigChanged) {
			try {
				config.writeAll();
			} catch (IOException e) {
				error("Config saving failed!", e);
			}
		}
	}

	public static ModInfo getModInfo(String group, String modid) {
		File modInfoDir = new File(CyanTrustServerModule.getModInfoDir(), group + "/" + modid);
		if (!modInfoDir.exists()) {
			return null;
		}
		try {
			return new ModInfo(modInfoDir);
		} catch (IOException e) {
			return null;
		}
	}

	public static String getDevGroup() {
		return configuration.get("mod-development-group");
	}
}
