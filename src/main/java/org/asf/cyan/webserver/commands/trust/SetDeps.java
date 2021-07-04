package org.asf.cyan.webserver.commands.trust;

import java.io.File;
import java.io.IOException;

import org.asf.connective.usermanager.api.AuthResult;
import org.asf.cyan.CyanTrustServerModule;
import org.asf.cyan.WebCommand;
import org.asf.cyan.webserver.commands.trust.services.ModValidityManager;
import org.asf.cyan.webserver.config.virtual.ModInfo;
import org.asf.cyan.webserver.config.virtual.SetDepsCommandConfig;

public class SetDeps extends WebCommand {

	@Override
	public String id() {
		return "set-mod-depfile";
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

		SetDepsCommandConfig ccfg = new SetDepsCommandConfig(getRequest());
		if (ccfg.group == null || ccfg.modid == null || (ccfg.artifacts == null && ccfg.repositories == null)) {
			getResponse().status = 400;
			getResponse().message = "Bad request";
			getResponse().setContent("text/plain", "Invalid content received, please format it in CCFG.\n");
			return;
		}

		AuthResult user = ModValidityManager.validateAccess(getRequest(), getResponse(), getContextRoot(), ccfg.group,
				ccfg.modid);
		if (user == null)
			return;

		File modDir = new File(CyanTrustServerModule.getModInfoDir(), ccfg.group + "/" + ccfg.modid);
		try {
			ModInfo info = new ModInfo(modDir);
			if (ccfg.artifacts != null)
				info.artifacts.putAll(ccfg.artifacts);
			if (ccfg.repositories != null)
				info.repositories.putAll(ccfg.repositories);
			info.save();
		} catch (IOException e) {
			getResponse().status = 503;
			getResponse().message = "Internal server error";
			return;
		}
	}

	@Override
	public WebCommand newInstance() {
		return new SetDeps();
	}

}
