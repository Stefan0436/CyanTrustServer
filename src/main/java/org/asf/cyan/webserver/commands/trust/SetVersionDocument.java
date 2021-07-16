package org.asf.cyan.webserver.commands.trust;

import java.io.File;
import java.io.IOException;

import org.asf.connective.usermanager.api.AuthResult;
import org.asf.cyan.CyanTrustServerModule;
import org.asf.cyan.WebCommand;
import org.asf.cyan.webserver.commands.trust.services.ModValidityManager;
import org.asf.cyan.webserver.config.virtual.ModInfo;
import org.asf.cyan.webserver.config.virtual.ModUpdateChannelConfig;

public class SetVersionDocument extends WebCommand {

	@Override
	public String id() {
		return "set-version-document";
	}

	@Override
	public void prepare() {
	}

	@Override
	public void run() {
		if (!getRequest().method.equals("POST")) {
			getResponse().status = 405;
			getResponse().message = "Method not allowed";
			return;
		}

		ModUpdateChannelConfig ccfg = new ModUpdateChannelConfig(getRequest());
		if (ccfg.group == null || ccfg.modid == null || ccfg.channels == null || ccfg.channelFiles == null) {
			getResponse().status = 400;
			getResponse().message = "Bad request";
			getResponse().setContent("text/plain", "Invalid content received, please format it in CCFG.\n");
			return;
		}
		for (String ch : ccfg.channels.values()) {
			if (!ccfg.channelFiles.containsKey(ch)) {
				getResponse().status = 400;
				getResponse().message = "Bad request";
				getResponse().setContent("text/plain", "Invalid content received, please format it in CCFG.\n");
				return;
			}
		}
		for (String ch : ccfg.channelFiles.keySet()) {
			if (!ccfg.channels.containsValue(ch) && !ch.equals("@fallback")) {
				getResponse().status = 400;
				getResponse().message = "Bad request";
				getResponse().setContent("text/plain", "Invalid content received, please format it in CCFG.\n");
				return;
			}
		}

		AuthResult user = ModValidityManager.validateAccess(getRequest(), getResponse(), getContextRoot(), ccfg.group,
				ccfg.modid);
		if (user == null)
			return;

		File modDir = new File(CyanTrustServerModule.getModInfoDir(), ccfg.group + "/" + ccfg.modid);
		try {
			ModInfo info = new ModInfo(modDir);
			info.channels.putAll(ccfg.channels);
			info.channelFiles.putAll(ccfg.channelFiles);
			info.save();
		} catch (IOException e) {
			getResponse().status = 503;
			getResponse().message = "Internal server error";
			return;
		}
	}

	@Override
	public WebCommand newInstance() {
		return new SetVersionDocument();
	}

}
