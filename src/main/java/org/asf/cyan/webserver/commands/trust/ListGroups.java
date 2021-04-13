package org.asf.cyan.webserver.commands.trust;

import org.asf.connective.usermanager.api.AuthResult;
import org.asf.cyan.WebCommand;
import org.asf.cyan.webserver.commands.trust.services.SessionManager;

public class ListGroups extends WebCommand {

	@Override
	public String id() {
		return "list-groups";
	}

	@Override
	public void prepare() {
	}

	@Override
	public void run() {
		AuthResult user = SessionManager.authenticate(getRequest(), getResponse(), getContextRoot());
		if (user == null)
			return;

		String[] groups = user.getUserStorage().get("mod-groups");
		if (groups == null) {
			getResponse().status = 404;
			getResponse().message = "File not found";
			getResponse().setContent("text/plain", "User does not have any mod groups registered.\n");
			return;
		}

		StringBuilder output = new StringBuilder();
		output.append("Mod groups this user owns:").append("\n");
		for (String group : groups) {
			int mods = user.getUserStorage().get("mod.group:" + group, String[].class).length;
			output.append(" - " + group + " - " + mods + " mod" + (mods == 1 ? "" : "s")).append("\n");
		}
		getResponse().setContent("text/plain", output.toString());
	}

	@Override
	public WebCommand newInstance() {
		return new ListGroups();
	}

}
