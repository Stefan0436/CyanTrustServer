package org.asf.cyan;

import org.asf.connective.usermanager.api.ArgumentSpecification;

public class WebArgumentSpecification extends ArgumentSpecification {

	protected int index = -1;

	public String getName() {
		return name;
	}

	public Class<?> getType() {
		return type;
	}

	public boolean isRequired() {
		return required;
	}

	public static WebArgumentSpecification create(String name, Class<?> type) {
		WebArgumentSpecification spec = new WebArgumentSpecification();
		spec.name = name;
		spec.type = type;
		return spec;
	}

	public static WebArgumentSpecification create(String name, Class<?> type, boolean required) {
		WebArgumentSpecification spec = new WebArgumentSpecification();
		spec.name = name;
		spec.type = type;
		spec.required = required;
		return spec;
	}

	public static WebArgumentSpecification create(String name, int index, Class<?> type) {
		WebArgumentSpecification spec = new WebArgumentSpecification();
		spec.name = name;
		spec.type = type;
		spec.index = index;
		return spec;
	}

	public static WebArgumentSpecification create(String name, int index, Class<?> type, boolean required) {
		WebArgumentSpecification spec = new WebArgumentSpecification();
		spec.name = name;
		spec.type = type;
		spec.required = required;
		spec.index = index;
		return spec;
	}

	public int getIndex() {
		return index;
	}

}
