package org.asf.cyan;

import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;

import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.http.ProviderContext;

public abstract class WebCommand {
	private ArrayList<WebArgument<?>> arguments = null;

	private String path;
	private Socket client;
	private String contextRoot;
	private HttpRequest request;
	private HttpResponse response;
	private ProviderContext context;
	private ConnectiveHTTPServer server;

	public abstract String id();

	public abstract void prepare();

	public abstract void run();

	public WebArgumentSpecification[] specification() {
		if (this.arguments == null) {
			this.arguments = new ArrayList<WebArgument<?>>();
			prepare();
		}

		return arguments.stream().map(t -> t.toSpecification()).toArray(t -> new WebArgumentSpecification[t]);
	}

	public WebCommand setup(Map<String, Object> arguments, HttpRequest request, HttpResponse response,
			ConnectiveHTTPServer server, ProviderContext context, String contextRoot, String path) {
		this.path = path;
		this.request = request;
		this.response = response;
		this.server = server;
		this.context = context;
		this.contextRoot = contextRoot;

		if (this.arguments == null) {
			this.arguments = new ArrayList<WebArgument<?>>();
			prepare();
		}

		arguments.forEach((k, v) -> {
			setInternal(k, v);
		});

		return this;
	}
	
	protected String getPath() {
		return path;
	}

	private void setInternal(String key, Object value) {
		for (WebArgument<?> arg : arguments) {
			if (arg.getName().equalsIgnoreCase(key) && arg.getType().isAssignableFrom(value.getClass())) {
				arg.set(value);
			}
		}
	}

	protected <T> WebArgument<T> registerArgument(String name, Class<T> type) {
		return registerArgument(name, -1, type);
	}

	protected <T> WebArgument<T> registerOptionalArgument(String name, Class<T> type) {
		return registerOptionalArgument(name, -1, type);
	}

	protected <T> WebArgument<T> registerArgument(String name, int index, Class<T> type) {
		WebArgument<T> arg = new WebArgument<T>();
		arg.setup(name, index, type, true);
		arguments.add(arg);
		return arg;
	}

	protected <T> WebArgument<T> registerOptionalArgument(String name, int index, Class<T> type) {
		WebArgument<T> arg = new WebArgument<T>();
		arg.setup(name, index, type, false);
		arguments.add(arg);
		return arg;
	}

	protected WebArgument<String> registerArgument(String name) {
		return registerArgument(name, String.class);
	}

	protected WebArgument<String> registerOptionalArgument(String name) {
		return registerOptionalArgument(name, String.class);
	}

	protected WebArgument<String> registerArgument(String name, int index) {
		return registerArgument(name, index, String.class);
	}

	protected WebArgument<String> registerOptionalArgument(String name, int index) {
		return registerOptionalArgument(name, index, String.class);
	}

	protected HttpRequest getRequest() {
		return request;
	}

	protected HttpResponse getResponse() {
		return response;
	}

	protected ProviderContext getContext() {
		return context;
	}

	protected String getContextRoot() {
		return contextRoot;
	}

	protected ConnectiveHTTPServer getServer() {
		return server;
	}

	protected Socket getClient() {
		return client;
	}

	@SuppressWarnings("unchecked")
	protected <T> T getValue(String argument) {
		return (T) getValue(argument, Object.class);
	}

	protected Object[] getAll(String argument) {
		return getAll(argument, Object.class);
	}

	@SuppressWarnings("unchecked")
	protected <T> T getValue(String argument, Class<T> type) {
		for (WebArgument<?> arg : arguments) {
			if (arg.getName().equalsIgnoreCase(argument) && type.isAssignableFrom(arg.getType())) {
				return (T) arg.getValue();
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	protected <T> T[] getAll(String argument, Class<T> type) {
		ArrayList<T> values = new ArrayList<T>();
		for (WebArgument<?> arg : arguments) {
			if (arg.getName().equalsIgnoreCase(argument) && type.isAssignableFrom(arg.getType())) {
				values.add((T) arg.getValue());
			}
		}
		return values.toArray(t -> (T[]) Array.newInstance(type, t));
	}

	protected boolean isPresent(String argument) {
		return isPresent(argument, Object.class);
	}

	protected boolean isPresent(String argument, Class<?> type) {
		for (WebArgument<?> arg : arguments) {
			if (arg.getName().equalsIgnoreCase(argument) && type.isAssignableFrom(arg.getType())) {
				return true;
			}
		}
		return false;
	}

	public abstract WebCommand newInstance();

}
