package org.asf.cyan.webserver.commands.trust.services;

import org.asf.connective.usermanager.api.AuthResult;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;

public class ModValidityManager {

	public static boolean validateNaming(HttpResponse response, String group, String modid) {
		if (!group.matches("^[A-Za-z0-9.]+$")) {
			response.status = 400;
			response.message = "Bad request";
			response.setContent("text/plain", "Invalid mod groupname.\n");
			return false;
		} else if (modid != null && !modid.matches("^[A-Za-z0-9]+$")) {
			response.status = 400;
			response.message = "Bad request";
			response.setContent("text/plain", "Invalid modid.\n");
			return false;
		}
		return true;
	}

	public static AuthResult validateAccess(HttpRequest request, HttpResponse response, String contextRoot,
			String group, String modid) {

		if (!validateNaming(response, group, modid))
			return null;

		AuthResult user = SessionManager.authenticate(request, response, contextRoot);
		if (user == null)
			return null;

		String[] groups = user.getUserStorage().get("mod-groups");
		if (groups != null) {
			for (String modgroup : groups) {
				if (group.equalsIgnoreCase(modgroup)) {
					if (modid == null)
						return user;

					for (String mod : user.getUserStorage().get("mod.group:" + modgroup, String[].class))
						if (mod.equalsIgnoreCase(modid))
							return user;

					response.status = 404;
					response.message = "File not found";
					response.setContent("text/plain", "Could not find mod " + group + ":" + modid + ".");
					return null;
				}
			}
		}

		response.status = 403;
		response.message = "Access denied";
		response.setContent("text/plain", "Access to mod group denied, user does not own it.");
		return null;
	}

}
