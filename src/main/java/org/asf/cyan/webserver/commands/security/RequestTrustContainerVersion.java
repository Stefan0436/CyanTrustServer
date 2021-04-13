package org.asf.cyan.webserver.commands.security;

import org.asf.cyan.WebCommand;
import org.asf.cyan.CyanTrustServerModule;
import org.asf.cyan.webserver.config.virtual.ModInfo;

public class RequestTrustContainerVersion extends WebCommand {

	@Override
	public String id() {
		return "request-trust-container-version";
	}

	@Override
	public void prepare() {
		this.registerArgument("group", 0);
		this.registerArgument("modid", 1);
		this.registerArgument("container", 2);
		this.registerArgument("version", 3);
	}

	@Override
	public void run() {
		String group = getValue("group");
		String modid = getValue("modid");
		String container = getValue("container");
		String version = getValue("version");

		if (!group.matches("^[A-Za-z0-9.]+$")) {
			getResponse().status = 400;
			getResponse().message = "Bad request";
			getResponse().setContent("text/plain", "Invalid mod groupname.\n");
			return;
		} else if (!modid.matches("^[A-Za-z0-9]+$")) {
			getResponse().status = 400;
			getResponse().message = "Bad request";
			getResponse().setContent("text/plain", "Invalid modid.\n");
			return;
		}

		ModInfo mod = CyanTrustServerModule.getModInfo(group, modid);
		if (mod == null) {
			getResponse().status = 404;
			getResponse().message = "File not found";
			getResponse().setContent("text/plain", "Could not locate requested mod information.\n");
			return;
		}
		
		if (container.toLowerCase().startsWith(group + "." + modid + ".")) {
			container = container.substring((group + "." + modid + ".").length());
		}

		if (!mod.trustContainers.containsKey(container)) {
			getResponse().status = 404;
			getResponse().message = "File not found";
			getResponse().setContent("text/plain", "Could not locate requested container.\n");
			return;
		}

		getResponse().setContent("text/plain", mod.trustContainers.get(container).versions.getOrDefault(version, version) + "\n");
	}

	@Override
	public WebCommand newInstance() {
		return new RequestTrustContainerVersion();
	}

}
