package org.asf.cyan.webserver.config.virtual;

import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;

public class TrustContainer extends Configuration<TrustContainer> {

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}
	
	public String name;
	public HashMap<String, String> versions = new HashMap<String, String>();
	public HashMap<String, String> hashes = new HashMap<String, String>();

}
