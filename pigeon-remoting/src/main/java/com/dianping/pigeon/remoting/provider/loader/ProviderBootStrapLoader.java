/**
 * Dianping.com Inc.
 * Copyright (c) 2003-2013 All Rights Reserved.
 */
package com.dianping.pigeon.remoting.provider.loader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dianping.pigeon.extension.ExtensionLoader;
import com.dianping.pigeon.registry.config.RegistryConfigLoader;
import com.dianping.pigeon.remoting.provider.Server;
import com.dianping.pigeon.remoting.provider.ServerFactory;

public final class ProviderBootStrapLoader {

	private static volatile Map<Integer, Server> servers = new ConcurrentHashMap<Integer, Server>();

	public static Server startup(int port) {
		Server server = servers.get(port);
		if (server == null) {
			synchronized (ProviderBootStrapLoader.class) {
				if (servers.get(port) == null) {
					RegistryConfigLoader.init();
					RequestProcessHandlerLoader.init();
					server = ExtensionLoader.getExtension(
							ServerFactory.class).createServer(port);
					if (server != null) {
						server.start();
					}
					servers.put(port, server);
				}
			}
		}
		return server;
	}

	public static void shutdown(int port) {
		RequestProcessHandlerLoader.clearServerInternalFilters();
		synchronized (ProviderBootStrapLoader.class) {
			Server server = servers.get(port);
			if (server != null) {
				server.stop();
				servers.remove(port);
			}
		}
	}
	
}
