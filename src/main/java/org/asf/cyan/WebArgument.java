package org.asf.cyan;

public class WebArgument<T> {

	protected WebArgument() {
	}

	private WebArgumentSpecification specification = null;
	private T value = null;
	private String name;
	private Class<T> type;

	@SuppressWarnings("unchecked")
	protected void set(Object value) {
		this.value = (T) value;
	}

	protected void setup(String name, int index, Class<T> type, boolean required) {
		this.name = name;
		this.type = type;
		specification = WebArgumentSpecification.create(name, index, type, required);
	}

	public WebArgumentSpecification toSpecification() {
		return specification;
	}

	public T getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	public Class<T> getType() {
		return type;
	}

}
