package org.asf.cyan.webserver.config.virtual;

import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;
import org.asf.rats.HttpRequest;

public class SetDepsCommandConfig extends Configuration<SetDepsCommandConfig> {

	public SetDepsCommandConfig(HttpRequest request) {
		if (request.headers.containsKey("Content-Length"))
			this.readAll(request.getRequestBody());
	}

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

	public String group = null;
	public String modid = null;

	public HashMap<String, String> repositories = null;
	public HashMap<String, HashMap<String, String>> artifacts = null;

}
