package org.asf.cyan.webserver.commands.security;

import org.asf.cyan.CyanTrustServerModule;
import org.asf.cyan.WebCommand;
import org.asf.cyan.webserver.config.virtual.ModInfo;

public class RequestModServerLocation extends WebCommand {

	@Override
	public String id() {
		return "request-server-location";
	}

	@Override
	public void prepare() {
		this.registerArgument("group", 0);
		this.registerArgument("modid", 1);
	}

	@Override
	public void run() {
		String group = getValue("group");
		String modid = getValue("modid");

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

		if (mod.trustServer != null) {
			getResponse().setContent("text/plain", mod.trustServer + "\n");
		} else {
			getResponse().status = 404;
			getResponse().message = "File not found";
			getResponse().setContent("text/plain", "Mod does not specify a specific server url for its trust store.\n");
			return;
		}
	}

	@Override
	public WebCommand newInstance() {
		return new RequestModServerLocation();
	}

}
