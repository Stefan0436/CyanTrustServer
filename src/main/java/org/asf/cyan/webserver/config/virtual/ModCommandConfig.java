package org.asf.cyan.webserver.config.virtual;

import org.asf.cyan.api.config.Configuration;
import org.asf.rats.HttpRequest;

public class ModCommandConfig extends Configuration<ModCommandConfig> {

	public ModCommandConfig(HttpRequest request) {
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

}
