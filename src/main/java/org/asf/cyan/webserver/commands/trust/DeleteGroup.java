package org.asf.cyan.webserver.commands.trust;

import java.io.File;
import java.io.IOException;

import org.asf.connective.usermanager.api.AuthResult;
import org.asf.cyan.CyanTrustServerModule;
import org.asf.cyan.WebCommand;
import org.asf.cyan.webserver.commands.trust.services.ModValidityManager;
import org.asf.cyan.webserver.config.virtual.GroupCommandConfig;

public class DeleteGroup extends WebCommand {

	@Override
	public String id() {
		return "delete-group";
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

		AuthResult user = ModValidityManager.validateAccess(getRequest(), getResponse(), getContextRoot(), ccfg.group,
				null);
		if (user == null)
			return;

		File groupDir = new File(CyanTrustServerModule.getModInfoDir(), ccfg.group);
		CyanTrustServerModule.deleteDir(groupDir);

		String[] groups = user.getUserStorage().get("mod-groups", String[].class);
		String[] newGroups = new String[groups.length - 1];

		int i = 0;
		for (String mgroup : groups)
			if (!mgroup.equals(ccfg.group))
				newGroups[i++] = mgroup;

		for (i = 0; i < groups.length; i++)
			groups[i] = null;
		groups = null;

		getResponse().status = 200;
		getResponse().message = "OK";

		user.getUserStorage().set("mod-groups", newGroups);
		user.getUserStorage().set("mod.group:" + ccfg.group, new String[] {});
		getResponse().setContent("text/plain", "Mod group has been deleted.\n");

		try {
			user.getUserStorage().write();
		} catch (IOException e) {
			getResponse().status = 503;
			getResponse().message = "Internal server error";
		}
	}

	@Override
	public WebCommand newInstance() {
		return new DeleteGroup();
	}

}
