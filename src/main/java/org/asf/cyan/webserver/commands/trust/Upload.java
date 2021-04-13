package org.asf.cyan.webserver.commands.trust;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.asf.connective.usermanager.api.AuthResult;
import org.asf.cyan.CyanTrustServerModule;
import org.asf.cyan.WebCommand;
import org.asf.cyan.webserver.commands.trust.services.ModValidityManager;
import org.asf.cyan.webserver.config.virtual.ModInfo;
import org.asf.cyan.webserver.config.virtual.TrustContainer;

public class Upload extends WebCommand {

	@Override
	public String id() {
		return "upload";
	}

	@Override
	public void prepare() {
		registerArgument("file");
		registerArgument("name");
		registerArgument("version");
		registerArgument("group", 0);
		registerArgument("modid", 1);
		registerOptionalArgument("trustname");
		registerOptionalArgument("modversion");
	}

	@Override
	public void run() {
		String version = getValue("version");
		String group = getValue("group");
		String modid = getValue("modid");
		String name = getValue("name");
		String path = getValue("file");

		if (!ModValidityManager.validateNaming(getResponse(), group, modid)) {
			return;
		}

		String trustName = name;
		String modVersion = version;
		if (getValue("trustname") != null) {
			trustName = getValue("trustname");
		}
		if (getValue("modversion") != null) {
			modVersion = getValue("modversion");
		}

		if (!getRequest().method.equals("PUT")) {
			getResponse().status = 405;
			getResponse().message = "Method not allowed";
			return;
		}

		if (getRequest().getRequestBodyStream() == null) {
			getResponse().status = 400;
			getResponse().message = "Bad request";
			return;
		}

		AuthResult user = ModValidityManager.validateAccess(getRequest(), getResponse(), getContextRoot(), group,
				modid);
		if (user == null)
			return;

		if (path.startsWith("/"))
			path = path.substring(1);

		String outFile = path;
		if (outFile.contains("."))
			outFile = outFile.substring(0, outFile.indexOf("."));
		if (path.contains("-" + version))
			outFile = outFile.substring(0, outFile.indexOf("-" + version));

		String outFileName = outFile + "-" + version + ".ctc";

		File trustDir = new File(CyanTrustServerModule.getModInfoDir(), group + "/" + modid + "/trust");
		ModInfo modinfo = CyanTrustServerModule.getModInfo(group, modid);
		if (modinfo == null) {
			getResponse().status = 404;
			getResponse().message = "File not found";
			getResponse().setContent("text/plain", "Could not locate requested mod information.\n");
			return;
		}

		TrustContainer container;
		if (!modinfo.trustContainers.containsKey(outFile)) {
			TrustContainer cont = new TrustContainer();
			cont.name = trustName;
			modinfo.trustContainers.put(outFile, cont);
			container = cont;
		} else {
			container = modinfo.trustContainers.get(outFile);
		}

		if (path.endsWith(".ctc")) {
			trustDir.mkdirs();
			File trustFile = new File(trustDir, outFileName);

			if (!trustFile.exists()) {
				getResponse().status = 201;
				getResponse().message = "Created";
			} else {
				getResponse().status = 200; // Java clients do not properly process it otherwise
				getResponse().message = "No content";
			}

			try {
				FileOutputStream strm = new FileOutputStream(trustFile);
				getRequest().transferRequestBody(strm);
				strm.close();
			} catch (IOException e) {
				getResponse().status = 503;
				getResponse().message = "Internal server error";
				return;
			}

			if (!version.equals(modVersion))
				container.versions.put(modVersion, version);

			try {
				modinfo.save();
			} catch (IOException e) {
				getResponse().status = 503;
				getResponse().message = "Internal server error";
				trustFile.delete();
			}
		} else if (path.endsWith(".ctc.sha256")) {
			String hash = getRequest().getRequestBody().replace("\r", "").replace("\t", "    ");
			if (hash.contains(" "))
				hash = hash.substring(0, hash.indexOf(" "));
			if (hash.contains("\n"))
				hash = hash.substring(0, hash.indexOf("\n"));

			if (!container.hashes.containsKey(version)) {
				getResponse().status = 201;
				getResponse().message = "Created";
			} else {
				getResponse().status = 200; // Java clients do not properly process it otherwise
				getResponse().message = "No content";
			}

			container.hashes.put(version, hash);
			try {
				modinfo.save();
			} catch (IOException e) {
				getResponse().status = 503;
				getResponse().message = "Internal server error";
			}
		} else {
			getResponse().status = 404;
			getResponse().message = "";
			getResponse().setContent("text/plain", "Unrecognized file, cannot save it.");
		}
	}

	@Override
	public WebCommand newInstance() {
		return new Upload();
	}

}
