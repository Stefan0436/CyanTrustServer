package org.asf.cyan.webserver.commands.security;

import org.asf.cyan.CyanTrustServerModule;
import org.asf.cyan.WebCommand;
import org.asf.cyan.webserver.config.virtual.ModInfo;
import org.asf.cyan.webserver.config.virtual.TrustContainer;

public class RequestTrustContainerName extends WebCommand {

	@Override
	public String id() {
		return "request-trust-container-name";
	}

	@Override
	public void prepare() {
		this.registerArgument("group", 0);
		this.registerArgument("modid", 1);
		this.registerArgument("container", 2);
	}

	@Override
	public void run() {
		String group = getValue("group");
		String modid = getValue("modid");
		String container = getValue("container");

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

		ModInfo mod = CyanTrustServerModule.getModInfo(group, modid);
		if (mod == null) {
			getResponse().status = 404;
			getResponse().message = "File not found";
			getResponse().setContent("text/plain", "Could not locate requested mod information.\n");
			return;
		}

		if (!mod.trustContainers.values().stream().anyMatch(t -> t.name.equals(container))) {
			getResponse().status = 404;
			getResponse().message = "File not found";
			getResponse().setContent("text/plain", "Could not locate requested container.\n");
			return;
		}

		for (String name : mod.trustContainers.keySet()) {
			TrustContainer cont = mod.trustContainers.get(name);
			if (cont.name.equals(container)) {
				getResponse().setContent("text/plain", group + "." + modid + "." + name + "\n");
				return;
			}
		}
	}

	@Override
	public WebCommand newInstance() {
		return new RequestTrustContainerName();
	}

}
