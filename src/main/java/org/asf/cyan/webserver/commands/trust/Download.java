package org.asf.cyan.webserver.commands.trust;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.asf.cyan.CyanTrustServerModule;
import org.asf.cyan.WebCommand;
import org.asf.cyan.webserver.config.virtual.ModInfo;

public class Download extends WebCommand {

	@Override
	public String id() {
		return "download";
	}

	@Override
	public void prepare() {
	}

	@Override
	public void run() {
		String str = this.getPath().substring(1);
		String group = "";
		String modid = "";
		String file = "";

		if (str.contains("/")) {
			file = str.substring(str.lastIndexOf("/") + 1);
			str = str.substring(0, str.lastIndexOf("/"));
		}
		str = str.replaceAll("/", ".");

		if (str.contains(".")) {
			group = str.substring(0, str.lastIndexOf("."));
			modid = str.substring(str.lastIndexOf(".") + 1);

			if (!group.matches("^[a-z0-9.]+$")) {
				getResponse().status = 400;
				getResponse().message = "Bad request";
				getResponse().setContent("text/plain", "Invalid mod groupname.\n");
				return;
			} else if (!modid.matches("^[a-z0-9]+$")) {
				getResponse().status = 400;
				getResponse().message = "Bad request";
				getResponse().setContent("text/plain", "Invalid modid.\n");
				return;
			}
		}

		File trustDir = new File(CyanTrustServerModule.getModInfoDir(), group + "/" + modid + "/trust");
		if (!group.isEmpty() && !modid.isEmpty()
				&& (trustDir.exists() || file.equalsIgnoreCase("mod.artifacts.deps"))) {
			ModInfo mod = CyanTrustServerModule.getModInfo(group, modid);
			if (mod == null) {
				getResponse().status = 404;
				getResponse().message = "File not found";
				getResponse().setContent("text/plain", "Could not locate requested mod information.\n");
				return;
			}

			if (file.equalsIgnoreCase("mod.artifacts.deps")) {
				if (mod.artifacts.size() != 0 || mod.repositories.size() != 0) {
					getResponse().setContent("text/ccfg", mod.toDepsFile());
				} else {
					getResponse().status = 404;
					getResponse().message = "File not found";
					getResponse().setContent("text/plain", "Could not locate requested file.\n");
					return;
				}
			} else if (file.equalsIgnoreCase("mod.artifacts.deps.sha256")) {
				String shafile = sha256HEX(mod.toDepsFile().getBytes()) + "  "
						+ file.substring(0, file.lastIndexOf("."));
				getResponse().setContent("text/plain", shafile + "\n");
			} else {
				if (file.endsWith(".sha256")) {
					String name = file.substring(0, file.lastIndexOf("."));
					String version = "";

					if (name.contains("."))
						name = name.substring(0, name.lastIndexOf("."));

					if (name.contains("-")) {
						version = name.substring(name.indexOf("-") + 1);
						name = name.substring(0, name.indexOf("-"));
					}

					String shafile = mod.trustContainers.get(name).hashes.get(version) + "  "
							+ file.substring(0, file.lastIndexOf("."));
					getResponse().setContent("text/plain", shafile + "\n");
					return;
				}
				File trustFile = new File(trustDir, file);
				if (!trustFile.exists()) {
					getResponse().status = 404;
					getResponse().message = "File not found";
					getResponse().setContent("text/plain", "Could not locate requested file.\n");
					return;
				}
				try {
					getResponse().setContent("application/octet-stream", new FileInputStream(trustFile));
				} catch (FileNotFoundException e) {
					getResponse().status = 503;
					getResponse().message = "Internal server error";
					return;
				}
			}
		} else {
			getResponse().status = 404;
			getResponse().message = "File not found";
			getResponse().setContent("text/plain", "Could not locate requested file.\n");
			return;
		}
	}

	private String sha256HEX(byte[] array) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		byte[] sha = digest.digest(array);
		StringBuilder result = new StringBuilder();
		for (byte aByte : sha) {
			result.append(String.format("%02x", aByte));
		}
		return result.toString();
	}

	@Override
	public WebCommand newInstance() {
		return new Download();
	}

}
