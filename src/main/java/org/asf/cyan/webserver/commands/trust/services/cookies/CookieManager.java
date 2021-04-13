package org.asf.cyan.webserver.commands.trust.services.cookies;

import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.asf.connective.usermanager.util.ParsingUtil;

public class CookieManager {
	protected static CookieManager implementation = new CookieManager();

	protected CookieCollection getCookiesImpl(HttpRequest request, HttpResponse response) {
		String[] cookieString = request.headers.getOrDefault("Cookie", "").split("; ");
		final HashMap<String, String> cookies;
		final HashMap<String, String> outputCookies = new HashMap<String, String>();

		String cookieQuery = "";
		for (String cookie : cookieString) {
			if (!cookieQuery.isEmpty())
				cookieQuery += "&";
			cookieQuery += cookie;
		}
		cookies = ParsingUtil.parseQuery(cookieQuery);

		return getCookies(name -> {
			return cookies.get(name);
		}, () -> {
			return cookies.keySet().toArray(t -> new String[t]);
		}, (cookie) -> {
			cookies.put(cookie.getName(), cookie.getValue());
			if (outputCookies.containsKey(cookie.getName())) {
				for (String header : response.headers.keySet()) {
					if (header.equalsIgnoreCase("Set-Cookie") || header.startsWith("Set-Cookie#")) {
						String headerValue = response.headers.get(header);

						String name = headerValue;
						String value = "";
						if (name.contains("=")) {
							value = name.substring(name.indexOf("=") + 1);
							name = name.substring(0, name.indexOf("="));
						}
						if (value.contains("; ")) {
							value = value.substring(0, value.indexOf("; "));
						}

						try {
							name = URLDecoder.decode(name, "UTF-8");
							value = URLDecoder.decode(value, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							continue;
						}

						if (name.equals(cookie.getName())) {
							response.headers.put(cookie.getCookieString(), value);
							return;
						}
					}
				}

				response.setHeader("Set-Cookie", cookie.getCookieString(), true);
			} else {
				response.setHeader("Set-Cookie", cookie.getCookieString(), true);
			}
		});
	}

	protected CookieCollection getCookiesImpl(Function<String, String> cookieProvider, Supplier<String[]> allCookies,
			Consumer<Cookie> cookieOutput) {
		CookieCollection collection = new CookieCollection();
		collection.assign(cookieOutput);

		for (String cookie : allCookies.get()) {
			collection.assign(cookie, new StringCookie(cookieProvider.apply(cookie)));
		}

		return collection;
	}

	public static CookieCollection getCookies(HttpRequest request, HttpResponse response) {
		return implementation.getCookiesImpl(request, response);
	}

	public static CookieCollection getCookies(Function<String, String> cookieProvider, Supplier<String[]> allCookies,
			Consumer<Cookie> cookieOutput) {
		return implementation.getCookiesImpl(cookieProvider, allCookies, cookieOutput);
	}

}
