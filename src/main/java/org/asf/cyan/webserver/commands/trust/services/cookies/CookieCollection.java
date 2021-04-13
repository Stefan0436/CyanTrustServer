package org.asf.cyan.webserver.commands.trust.services.cookies;

import java.util.Iterator;
import java.util.function.Consumer;

public class CookieCollection implements Iterable<Cookie> {
	
	protected class CookieEntry {
		public Cookie cookie;
		public CookieEntry next;
	}

	protected class CookieIterator implements Iterator<Cookie> {
		public CookieEntry current;

		public CookieIterator(CookieEntry first) {
			current = first;
		}

		@Override
		public boolean hasNext() {
			return current != null;
		}

		@Override
		public Cookie next() {
			CookieEntry ent = current;
			current = ent.next;
			return ent.cookie;
		}
	}

	protected CookieCollection() {
	}

	protected CookieEntry first;
	protected Consumer<Cookie> setValue;

	protected void assign(Consumer<Cookie> setValue) {
		this.setValue = setValue;
	}

	public Cookie get(String name) {
		CookieEntry ent = first;
		while (ent != null) {
			if (ent.cookie.getName().equals(name)) {
				return ent.cookie;
			}
			ent = ent.next;
		}

		return null;
	}

	public String getValue(String name) {
		CookieEntry ent = first;
		while (ent != null) {
			if (ent.cookie.getName().equals(name)) {
				return ent.cookie.getValue();
			}
			ent = ent.next;
		}

		return null;
	}

	public boolean contains(String name) {
		CookieEntry ent = first;
		while (ent != null) {
			if (ent.cookie.getName().equals(name)) {
				return true;
			}
			ent = ent.next;
		}
		return false;
	}

	public Cookie set(String name, String value) {
		return setInternal(name, new StringCookie(value));
	}

	private Cookie setInternal(String name, Cookie input) {
		Cookie cookie = assign(name, input);
		setValue.accept(cookie);
		return cookie;
	}

	public Cookie set(String name, Cookie value) {
		return setInternal(name, value);
	}

	@Override
	public Iterator<Cookie> iterator() {
		return new CookieIterator(first);
	}

	protected Cookie assign(String name, Cookie input) {
		Cookie cookie = get(name);
		if (cookie == null) {
			cookie = Cookie.create(name);

			CookieEntry ent = first;
			if (first != null) {
				while (ent.next != null) {
					ent = ent.next;
				}
				ent.next = new CookieEntry();
				ent = ent.next;
			} else {
				first = new CookieEntry();
				ent = first;
			}
			ent.cookie = cookie;
		}

		cookie.setValue(input);
		
		return cookie;
	}

}
