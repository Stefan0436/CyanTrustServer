package org.asf.cyan.webserver.commands.trust.services;

import java.util.HashMap;

import org.asf.connective.usermanager.api.AuthResult;
import org.asf.connective.usermanager.api.IAuthService;
import org.asf.connective.usermanager.util.ParsingUtil;
import org.asf.cyan.webserver.commands.trust.services.cookies.Cookie;
import org.asf.cyan.webserver.commands.trust.services.cookies.CookieManager;
import org.asf.cyan.webserver.commands.trust.services.cookies.Cookie.CookieOption;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;

public class LoginService implements IAuthService {

	@Override
	public String path() {
		return "cyan.trust.service.login";
	}

	@Override
	public String name() {
		return "cyan.trust.service.login";
	}

	@Override
	public void run(AuthResult result, HttpRequest request, HttpResponse response, ConnectiveHTTPServer server) {
		HashMap<String, String> query = ParsingUtil.parseQuery(request.query);

		if (!query.containsKey("returnpage")) {
			response.status = 400;
			response.message = "Bad request";
			return;
		}

		String key = SessionManager.genKey(result);
		CookieManager.getCookies(request, response).set("cyan-upload-authorization-key",
				new Cookie().setValue(key).setOption(CookieOption.PATH, "/").setExpires(SessionManager.getExpiry(key)));

		response.status = 302;
		response.message = "File found";
		response.setHeader("Location", query.get("returnpage"));
	}

}
