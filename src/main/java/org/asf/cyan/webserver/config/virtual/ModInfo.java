package org.asf.cyan.webserver.config.virtual;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;

public class ModInfo extends Configuration<ModInfo> {

	private File modDir;
	
	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

	public ModInfo(File modDir) throws IOException {
		this.modDir = modDir;
		if (modDir.exists()) {
			for (File ccfg : modDir.listFiles((file) -> !file.isDirectory() && file.getName().endsWith(".ccfg"))) {
				readAll(Files.readString(ccfg.toPath()));
			}
		}
	}

	public void save() throws IOException {
		modDir.mkdirs();
		if (trustServer != null) {
			LocationConfig conf = new LocationConfig();
			conf.trustServer = trustServer;
			Files.writeString(new File(modDir, "location.ccfg").toPath(), conf.toString());
		}
		
		ContainerConfig conf = new ContainerConfig();
		conf.trustContainers.putAll(trustContainers);
		Files.writeString(new File(modDir, "containers.ccfg").toPath(), conf.toString());
	}

	@SuppressWarnings("unused")
	private class LocationConfig extends Configuration<LocationConfig> {
		@Override
		public String filename() {
			return null;
		}

		@Override
		public String folder() {
			return null;
		}

		public String trustServer = null;
	}

	private class ContainerConfig extends Configuration<ContainerConfig> {
		@Override
		public String filename() {
			return null;
		}

		@Override
		public String folder() {
			return null;
		}

		public HashMap<String, TrustContainer> trustContainers = new HashMap<String, TrustContainer>();
	}

	public String trustServer = null;
	public HashMap<String, TrustContainer> trustContainers = new HashMap<String, TrustContainer>();

}
