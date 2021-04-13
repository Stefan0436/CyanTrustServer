package org.asf.cyan.webserver.commands.trust;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import org.asf.connective.usermanager.api.AuthResult;
import org.asf.cyan.CyanTrustServerModule;
import org.asf.cyan.WebCommand;
import org.asf.cyan.webserver.commands.trust.services.ModValidityManager;
import org.asf.cyan.webserver.config.virtual.ModCommandConfig;

public class RegisterModid extends WebCommand {

	@Override
	public String id() {
		return "register-modid";
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

		ModCommandConfig ccfg = new ModCommandConfig(getRequest());
		if (ccfg.group == null || ccfg.modid == null) {
			getResponse().status = 400;
			getResponse().message = "Bad request";
			getResponse().setContent("text/plain", "Invalid content received, please format it in CCFG.\n");
			return;
		}
		if (!ModValidityManager.validateNaming(getResponse(), ccfg.group, ccfg.modid)) {
			return;
		}

		AuthResult user = ModValidityManager.validateAccess(getRequest(), getResponse(), getContextRoot(), ccfg.group,
				null);
		if (user == null)
			return;

		String[] modids = user.getUserStorage().get("mod.group:" + ccfg.group);
		if (Stream.of(modids).anyMatch(t -> t.equals(ccfg.modid))) {
			getResponse().status = 403;
			getResponse().message = "Access denied";
			getResponse().setContent("text/plain", "ModID already registered.\n");
			return;
		}
		if (CyanTrustServerModule.getMaxMods() != -1 && modids.length >= CyanTrustServerModule.getMaxMods()) {
			getResponse().status = 403;
			getResponse().message = "Access denied";
			getResponse().setContent("text/plain", "Maximum amount of mods for this group has been exceeded.\n");
			return;
		}

		File modDir = new File(CyanTrustServerModule.getModInfoDir(), ccfg.group + "/" + ccfg.modid);
		modDir.mkdirs();
		String[] newIds = new String[modids.length + 1];
		int i = 0;
		for (String id : modids)
			newIds[i++] = id;
		newIds[i] = ccfg.modid;

		user.getUserStorage().set("mod.group:" + ccfg.group, newIds);
		for (i = 0; i < modids.length; i++)
			modids[i] = null;
		modids = null;

		getResponse().status = 201;
		getResponse().message = "Created";
		getResponse().setContent("text/plain", "ModID has been registered.\n");
		try {
			user.getUserStorage().write();
		} catch (IOException e) {
			getResponse().status = 503;
			getResponse().message = "Internal server error";
			getResponse().setContent("text/plain", "Internal server error.\n");
		}
	}

	@Override
	public WebCommand newInstance() {
		return new RegisterModid();
	}

}
