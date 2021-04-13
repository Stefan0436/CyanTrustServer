package org.asf.cyan.webserver.commands.trust.services;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AuthResult;
import org.asf.connective.usermanager.api.IAuthFrontend;
import org.asf.cyan.CyanTrustServerModule;
import org.asf.cyan.webserver.commands.trust.services.cookies.CookieCollection;
import org.asf.cyan.webserver.commands.trust.services.cookies.CookieManager;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.Memory;

public class SessionManager {

	private static HashMap<String, Session> keys = new HashMap<String, Session>();

	private static class Session {

		public AuthResult user;
		public Date expiry;

		public Session(AuthResult user) {
			this.user = user;

			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.HOUR_OF_DAY, 2);
			expiry = cal.getTime();
		}

	}

	static {
		new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(10 * 60 * 1000);
				} catch (InterruptedException e) {
					break;
				}
				for (String key : new ArrayList<String>(keys.keySet())) {
					while (true) {
						try {
							Session ses = keys.get(key);
							if (ses != null) {
								if (new Date().after(ses.expiry)) {
									keys.remove(key);
								}
							}
							break;
						} catch (Exception e) {

						}
					}
				}
			}
		}, "Cyan upload auth cleanup").start();
	}

	private static AuthResult getAuth(CookieCollection cookies) {
		if (!cookies.contains("cyan-upload-authorization-key"))
			return null;

		String key = cookies.getValue("cyan-upload-authorization-key");

		Session session = keys.get(key);
		if (session == null)
			return null;

		return session.user;
	}

	public static String genKey(AuthResult user) {
		String key = System.currentTimeMillis() + "-" + UUID.randomUUID().toString() + "-"
				+ UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString()
				+ "-" + UUID.randomUUID().toString() + "-" + System.currentTimeMillis();

		while (keys.containsKey(key)) {
			key = System.currentTimeMillis() + "-" + UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString()
					+ "-" + UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString() + "-"
					+ UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
		}

		keys.put(key, new Session(user));
		return key;
	}

	public static void deleteSession(AuthResult user) {
		for (String key : new ArrayList<String>(keys.keySet())) {
			Session session = keys.get(key);
			if (session.user.getGroup().equals(user.getGroup())
					&& session.user.getUsername().equals(user.getUsername()))
				keys.remove(key);
		}
	}

	public static AuthResult authenticate(HttpRequest request, HttpResponse response, String contextRoot) {
		if (request.headers.containsKey("Authorization") || request.headers.containsKey("X-Use-HTTP-Authentication")) {
			try {
				AuthResult user = Memory.getInstance().get("usermanager.auth.frontend").getValue(IAuthFrontend.class)
						.authenticate(CyanTrustServerModule.getDevGroup(), request, response);
				response.body = null;
				if (!user.success())
					return null;
				
				return user;
			} catch (IOException e) {
			}
		}

		String url = "/" + contextRoot + "/" + UserManagerModule.getBase() + "/" + UserManagerModule.getAuthCommand();
		while (url.contains("//")) {
			url = url.replace("//", "/");
		}

		CookieCollection cookies = CookieManager.getCookies(request, response);
		AuthResult user = getAuth(cookies);
		try {
			if (!Memory.getInstance().get("usermanager.auth.frontend").getValue(IAuthFrontend.class)
					.check(CyanTrustServerModule.getDevGroup(), request, response) || user == null) {
				response.status = 302;
				response.message = "Authentication required";
				response.setHeader("Location",
						url + "?group=" + CyanTrustServerModule.getDevGroup()
								+ "&service=cyan.trust.service.login&returnpage=" + URLEncoder.encode(
										request.path + (request.query.isEmpty() ? "" : "?" + request.query), "UTF-8"));
				return null;
			}
		} catch (IOException e) {
			response.status = 503;
			response.message = "Internal server error";
			return null;
		}

		return user;
	}

	public static Date getExpiry(String key) {
		Session session = keys.get(key);
		if (session == null)
			return null;

		return session.expiry;
	}

}
