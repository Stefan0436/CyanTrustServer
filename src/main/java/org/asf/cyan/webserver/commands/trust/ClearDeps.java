package org.asf.cyan.webserver.commands.trust;

import java.io.File;
import java.io.IOException;

import org.asf.connective.usermanager.api.AuthResult;
import org.asf.cyan.CyanTrustServerModule;
import org.asf.cyan.WebCommand;
import org.asf.cyan.webserver.commands.trust.services.ModValidityManager;
import org.asf.cyan.webserver.config.virtual.ModInfo;
import org.asf.cyan.webserver.config.virtual.ModCommandConfig;

public class ClearDeps extends WebCommand {

	@Override
	public String id() {
		return "clear-mod-depfile";
	}

	@Override
	public void prepare() {
	}

	@Override
	public void run() {
		ModCommandConfig ccfg = new ModCommandConfig(getRequest());
		if (ccfg.group == null || ccfg.modid == null) {
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
			info.repositories.clear();
			info.artifacts.clear();
			info.save();
			
			File locFile = new File(modDir, "artifacts.ccfg");
			if (locFile.exists())
				locFile.delete();
		} catch (IOException e) {
			getResponse().status = 503;
			getResponse().message = "Internal server error";
			return;
		}
	}

	@Override
	public WebCommand newInstance() {
		return new ClearDeps();
	}

}
