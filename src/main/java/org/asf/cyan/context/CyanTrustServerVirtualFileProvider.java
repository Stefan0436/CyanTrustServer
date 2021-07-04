package org.asf.cyan.context;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.asf.connective.usermanager.util.ParsingUtil;
import org.asf.cyan.CyanTrustServerModule;
import org.asf.cyan.WebArgumentSpecification;
import org.asf.cyan.WebCommand;
import org.asf.cyan.api.config.serializing.internal.Splitter;
import org.asf.cyan.api.packet.PacketEntry;
import org.asf.cyan.api.packet.PacketParser;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.http.ProviderContext;
import org.asf.rats.http.providers.IContextProviderExtension;
import org.asf.rats.http.providers.IContextRootProviderExtension;
import org.asf.rats.http.providers.IServerProviderExtension;
import org.asf.rats.http.providers.IVirtualFileProvider;

import org.asf.cyan.webserver.commands.security.RequestModServerLocation;
import org.asf.cyan.webserver.commands.security.RequestTrustContainerName;
import org.asf.cyan.webserver.commands.security.RequestTrustContainerVersion;
import org.asf.cyan.webserver.commands.trust.ClearDeps;
import org.asf.cyan.webserver.commands.trust.CreateGroup;
import org.asf.cyan.webserver.commands.trust.DeleteGroup;
import org.asf.cyan.webserver.commands.trust.Download;
import org.asf.cyan.webserver.commands.trust.ListGroups;
import org.asf.cyan.webserver.commands.trust.ListModids;
import org.asf.cyan.webserver.commands.trust.RegisterModid;
import org.asf.cyan.webserver.commands.trust.SetDeps;
import org.asf.cyan.webserver.commands.trust.SetLocation;
import org.asf.cyan.webserver.commands.trust.UnregisterModid;
import org.asf.cyan.webserver.commands.trust.Upload;

public class CyanTrustServerVirtualFileProvider implements IVirtualFileProvider, IContextProviderExtension,
		IContextRootProviderExtension, IServerProviderExtension {

	protected static class EntryTypeManager extends PacketParser {
		private static EntryTypeManager inst = new EntryTypeManager();

		public static void registerEntryType(PacketEntry<?> entry) {
			inst.registerType(entry);
		}

		public static PacketEntry<?> getEntryForType(long type) {
			try {
				@SuppressWarnings("rawtypes")
				Constructor<? extends PacketEntry> ctor = inst.entryTypes.get(type).getDeclaredConstructor();
				ctor.setAccessible(true);
				return ctor.newInstance();
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				return null;
			}
		}
	}

	@Override
	public boolean supportsUpload() {
		return true;
	}

	private static ArrayList<WebCommand> modCommands = new ArrayList<WebCommand>();
	private static ArrayList<WebCommand> trustCommands = new ArrayList<WebCommand>();

	static {
		modCommands.add(new RequestModServerLocation());
		modCommands.add(new RequestTrustContainerName());
		modCommands.add(new RequestTrustContainerVersion());

		trustCommands.add(new Download());
		trustCommands.add(new Upload());
		trustCommands.add(new CreateGroup());
		trustCommands.add(new RegisterModid());
		trustCommands.add(new ListModids());
		trustCommands.add(new ListGroups());
		trustCommands.add(new UnregisterModid());
		trustCommands.add(new DeleteGroup());
		trustCommands.add(new SetLocation());
		trustCommands.add(new SetDeps());
		trustCommands.add(new ClearDeps());
	}

	private WebCommand runCommand(WebCommand command, Supplier<String[]> argumentKeyProvider,
			Function<String, String> argumentValueProvider, Supplier<String[]> indexedArgumentProvider,
			HttpRequest request, HttpResponse response, String path) {
		ArrayList<String> keys = new ArrayList<String>(Arrays.asList(argumentKeyProvider.get()));
		HashMap<String, String> indexedValues = new HashMap<String, String>();

		int i = 0;
		for (String arg : indexedArgumentProvider.get()) {
			for (WebArgumentSpecification spec : command.specification()) {
				if (spec.getIndex() == i) {
					indexedValues.put(spec.getName(), arg);
					keys.add(spec.getName());
				}
			}
			i++;
		}

		for (WebArgumentSpecification spec : command.specification()) {
			if (spec.isRequired() && !keys.contains(spec.getName())) {
				return null;
			}
		}

		HashMap<String, Object> arguments = new HashMap<String, Object>();
		for (String key : keys) {
			Optional<WebArgumentSpecification> optSpec = Stream.of(command.specification())
					.filter(t -> t.getName().equals(key)).findFirst();
			if (optSpec.isEmpty())
				return null;

			WebArgumentSpecification spec = optSpec.get();

			String val = "";
			if (spec.getIndex() != -1 && spec.getIndex() < indexedValues.size()
					&& indexedValues.containsKey(spec.getName())) {
				val = indexedValues.get(spec.getName());
			} else {
				val = argumentValueProvider.apply(key);
			}

			if (spec.getType().getTypeName().equals(String.class.getTypeName())) {
				arguments.put(key, val);
			} else {
				try {
					byte[] data = Base64.getDecoder().decode(val);
					PacketEntry<?> ent = EntryTypeManager
							.getEntryForType(ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 8)).getLong());

					ByteArrayInputStream strm = new ByteArrayInputStream(Arrays.copyOfRange(data, 8, data.length));
					try {
						arguments.put(key, ent.importStream(strm, data.length - 8).get());
					} catch (IOException e) {
					}
				} catch (Exception e) {
				}
			}
		}

		WebCommand inst = command.newInstance().setup(arguments, request, response, server, context, contextRoot, path);
		inst.run();
		return inst;
	}

	private String contextRoot;
	private ConnectiveHTTPServer server;
	private ProviderContext context;

	@Override
	public boolean match(String path, HttpRequest request) {
		String securityPath = CyanTrustServerModule.getSecurityPath();
		String trustRepoPath = CyanTrustServerModule.getTrustRepositoryPath();

		if (path.equalsIgnoreCase(securityPath) || path.toLowerCase().startsWith(securityPath.toLowerCase() + "/")
				|| path.equalsIgnoreCase(trustRepoPath)
				|| path.toLowerCase().startsWith(trustRepoPath.toLowerCase() + "/")) {
			return true;
		}

		return false;
	}

	@Override
	public IVirtualFileProvider newInstance() {
		return new CyanTrustServerVirtualFileProvider();
	}

	@Override
	public void process(String path, String uploadMediaType, HttpRequest request, HttpResponse response, Socket client,
			String method) {
		response.body = null;

		String securityPath = CyanTrustServerModule.getSecurityPath();
		String trustRepoPath = CyanTrustServerModule.getTrustRepositoryPath();

		if (path.equalsIgnoreCase(securityPath) || path.toLowerCase().startsWith(securityPath.toLowerCase() + "/")) {
			String command = path.substring(securityPath.length());
			exec(command, request, response, securityPath, modCommands);
		} else {
			String command = path.substring(trustRepoPath.length());
			exec(command, request, response, trustRepoPath, trustCommands);
		}

		if (response.body == null)
			response.setContent("text/plain",
					(response.message.endsWith(".") ? response.message : response.message + ".") + "\n");
	}

	private void exec(String command, HttpRequest request, HttpResponse response, String basePath,
			ArrayList<WebCommand> cmds) {
		String subCommand = "";

		if (command.startsWith("/"))
			command = command.substring(1);

		if (command.contains("/")) {
			subCommand = command.substring(command.indexOf("/") + 1);

			if (subCommand.startsWith("/"))
				subCommand = subCommand.substring(1);

			command = command.substring(0, command.indexOf("/"));
		}

		boolean found = false;
		for (WebCommand cmd : cmds) {
			if (cmd.id().equalsIgnoreCase(command)) {
				execCommand(basePath, cmd, subCommand, response, request);
				found = true;
				break;
			}
		}

		if (!found) {
			response.status = 404;
			response.message = "Command not recognized";
		}
	}

	private void execCommand(String commandBase, WebCommand cmd, String path, HttpResponse response,
			HttpRequest request) {
		HashMap<String, String> query = ParsingUtil.parseQuery(request.query);

		response.status = 200;
		response.message = "OK";
		final String subC = path;

		if (!path.startsWith("/"))
			path = "/" + path;

		WebCommand out = this.runCommand(cmd, () -> {
			return query.keySet().toArray(t -> new String[t]);
		}, (key) -> query.get(key), () -> {
			return Splitter.split(subC, '/');
		}, request, response, path);

		if (out == null) {
			response.status = 400;
			response.message = "Bad request";
			StringBuilder syntax = new StringBuilder();

			syntax.append(commandBase + "/" + cmd.id());
			for (WebArgumentSpecification spec : cmd.specification()) {
				if (spec.getIndex() != -1) {
					syntax.append("/");
					if (!spec.isRequired()) {
						syntax.append("[" + spec.getName() + "]");
					} else {
						syntax.append("<" + spec.getName() + ">");
					}
				}
			}
			boolean first = true;
			for (WebArgumentSpecification spec : cmd.specification()) {
				if (spec.getIndex() == -1) {
					if (!spec.isRequired())
						syntax.append("[");
					if (first) {
						syntax.append("?");
						first = false;
					} else {
						syntax.append("&");
					}

					syntax.append(spec.getName());
					syntax.append("=");
					syntax.append("<");
					if (!spec.getType().getTypeName().equals(String.class.getTypeName())) {
						syntax.append("(base64-encoded)");
					}
					syntax.append(spec.getType().getSimpleName());
					syntax.append(">");

					if (!spec.isRequired())
						syntax.append("]");
				}
			}

			response.setContent("text/plain", "Invalid command usage, syntax:\n" + syntax + "\n");
		}
	}

	@Override
	public void provide(ConnectiveHTTPServer server) {
		this.server = server;
	}

	@Override
	public void provideVirtualRoot(String virtualRoot) {
		this.contextRoot = virtualRoot;
	}

	@Override
	public void provide(ProviderContext context) {
		this.context = context;
	}

}
