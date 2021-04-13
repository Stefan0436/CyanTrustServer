package org.asf.cyan.webserver.commands.trust;

import org.asf.connective.usermanager.api.AuthResult;
import org.asf.cyan.WebCommand;
import org.asf.cyan.webserver.commands.trust.services.ModValidityManager;

public class ListModids extends WebCommand {

	@Override
	public String id() {
		return "list-modids";
	}

	@Override
	public void prepare() {
		registerArgument("group", 0);
	}

	@Override
	public void run() {
		String group = getValue("group");
		if (!ModValidityManager.validateNaming(getResponse(), group, null)) {
			return;
		}

		AuthResult user = ModValidityManager.validateAccess(getRequest(), getResponse(), getContextRoot(), group, null);
		if (user == null)
			return;

		String[] modids = user.getUserStorage().get("mod.group:" + group);
		StringBuilder output = new StringBuilder();
		output.append("Mods in the " + group + " group:").append("\n");
		for (String mod : modids) {
			output.append(" - " + mod).append("\n");
		}
		getResponse().setContent("text/plain", output.toString());
	}

	@Override
	public WebCommand newInstance() {
		return new ListModids();
	}

}
