package org.asf.cyan.webserver.commands.trust;

import java.io.File;
import java.io.IOException;

import org.asf.connective.usermanager.api.AuthResult;
import org.asf.cyan.CyanTrustServerModule;
import org.asf.cyan.WebCommand;
import org.asf.cyan.webserver.commands.trust.services.SessionManager;
import org.asf.cyan.webserver.config.virtual.GroupCommandConfig;

public class CreateGroup extends WebCommand {

	@Override
	public String id() {
		return "create-group";
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

		GroupCommandConfig ccfg = new GroupCommandConfig(getRequest());
		if (ccfg.group == null) {
			getResponse().status = 400;
			getResponse().message = "Bad request";
			getResponse().setContent("text/plain", "Invalid content received, please format it in CCFG.\n");
			return;
		}
		if (!ccfg.group.matches("^[a-z0-9.]+$")) {
			getResponse().status = 400;
			getResponse().message = "Bad request";
			getResponse().setContent("text/plain", "Invalid groupname.\n");
			return;
		}

		AuthResult user = SessionManager.authenticate(getRequest(), getResponse(), getContextRoot());
		if (user == null)
			return;

		File groupDir = new File(CyanTrustServerModule.getModInfoDir(), ccfg.group);
		if (groupDir.exists()) {
			getResponse().status = 403;
			getResponse().message = "Access denied";
			getResponse().setContent("text/plain", "Mod group already exists.\n");
			return;
		}

		String[] groups = user.getUserStorage().get("mod-groups", String[].class);
		if (groups == null)
			groups = new String[0];

		if (CyanTrustServerModule.getMaxModGroups() != -1 && groups.length >= CyanTrustServerModule.getMaxModGroups()) {
			getResponse().status = 403;
			getResponse().message = "Access denied";
			getResponse().setContent("text/plain", "Maximum amount of owned mod groups has been exceeded.\n");
			return;
		}

		groupDir.mkdirs();
		String[] newGroups = new String[groups.length + 1];

		int i = 0;
		for (String mgroup : groups)
			newGroups[i++] = mgroup;
		newGroups[i] = ccfg.group;
		for (i = 0; i < groups.length; i++)
			groups[i] = null;
		groups = null;

		getResponse().status = 201;
		getResponse().message = "Created";
		user.getUserStorage().set("mod-groups", newGroups);
		user.getUserStorage().set("mod.group:" + ccfg.group, new String[] {});

		try {
			user.getUserStorage().write();
		} catch (IOException e) {
			getResponse().status = 503;
			getResponse().message = "Internal server error";
		}
	}

	@Override
	public WebCommand newInstance() {
		return new CreateGroup();
	}

}
