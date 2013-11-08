/**
 * Dianping.com Inc.
 * Copyright (c) 2003-2013 All Rights Reserved.
 */
package com.dianping.pigeon.remoting.provider.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.dianping.pigeon.component.invocation.InvocationContext;
import com.dianping.pigeon.component.invocation.InvocationResponse;
import com.dianping.pigeon.remoting.common.config.RemotingConfigurer;
import com.dianping.pigeon.remoting.common.filter.ServiceInvocationFilter;
import com.dianping.pigeon.remoting.common.filter.ServiceInvocationHandler;
import com.dianping.pigeon.remoting.common.util.Constants;
import com.dianping.pigeon.remoting.provider.component.ProcessPhase;
import com.dianping.pigeon.remoting.provider.component.context.ProviderContext;
import com.dianping.pigeon.remoting.provider.filter.BusinessProcessFilter;
import com.dianping.pigeon.remoting.provider.filter.ContextTransferProcessFilter;
import com.dianping.pigeon.remoting.provider.filter.ExceptionProcessFilter;
import com.dianping.pigeon.remoting.provider.filter.HeartbeatProcessFilter;
import com.dianping.pigeon.remoting.provider.filter.MonitorProcessFilter;
import com.dianping.pigeon.remoting.provider.filter.WriteResponseProcessFilter;

public final class RequestProcessHandlerLoader {

	private static final Logger logger = Logger.getLogger(RequestProcessHandlerLoader.class);

	private static Map<ProcessPhase, List<ServiceInvocationFilter<ProviderContext>>> bizProcessFilters = new LinkedHashMap<ProcessPhase, List<ServiceInvocationFilter<ProviderContext>>>();

	private static Map<ProcessPhase, List<ServiceInvocationFilter<ProviderContext>>> heartBeatProcessFilters = new LinkedHashMap<ProcessPhase, List<ServiceInvocationFilter<ProviderContext>>>();

	/**
	 * 运维用来做pigeon服务器检查的
	 */
	private static Map<ProcessPhase, List<ServiceInvocationFilter<ProviderContext>>> echoProcessFilters = new LinkedHashMap<ProcessPhase, List<ServiceInvocationFilter<ProviderContext>>>();

	private static ServiceInvocationHandler bizInvocationHandler = null;

	private static ServiceInvocationHandler heartBeatInvocationHandler = null;

	private static ServiceInvocationHandler echoInvocationHandler = null;

	public static ServiceInvocationHandler selectInvocationHandler(int messageType) {
		if (Constants.MESSAGE_TYPE_HEART == messageType) {
			return heartBeatInvocationHandler;
		} else if (Constants.MESSAGE_TYPE_ECHO == messageType) {
			return echoInvocationHandler;
		} else {
			return bizInvocationHandler;
		}
	}

	public static void init() {
		if (RemotingConfigurer.isMonitorEnabled()) {
			registerBizProcessFilter(ProcessPhase.Before_Write, new MonitorProcessFilter());
		}
		registerBizProcessFilter(ProcessPhase.Write, new WriteResponseProcessFilter());
		registerBizProcessFilter(ProcessPhase.Before_Execute, new ContextTransferProcessFilter());
		registerBizProcessFilter(ProcessPhase.Before_Execute, new ExceptionProcessFilter());
		registerBizProcessFilter(ProcessPhase.Execute, new BusinessProcessFilter());
		bizInvocationHandler = createInvocationHandler(bizProcessFilters);

		registerHeartBeatProcessFilter(ProcessPhase.Write, new WriteResponseProcessFilter());
		registerHeartBeatProcessFilter(ProcessPhase.Before_Execute, new HeartbeatProcessFilter());
		heartBeatInvocationHandler = createInvocationHandler(heartBeatProcessFilters);

		// registerEchoProcessFilter(ProcessPhase.Write, new
		// WriteResponseProcessFilter());
		// registerEchoProcessFilter(ProcessPhase.Before_Execute, new
		// EchoProcessFilter());
		// echoInvocationHandler = createInvocationHandler(echoProcessFilters);
	}

	@SuppressWarnings({ "rawtypes" })
	private static <K, V extends ServiceInvocationFilter> ServiceInvocationHandler createInvocationHandler(
			Map<K, List<V>> internalFilters) {
		Map<K, List<V>> mergedFilters = new LinkedHashMap<K, List<V>>(internalFilters);
		ServiceInvocationHandler last = null;
		List<V> filterList = new ArrayList<V>();
		for (Map.Entry<K, List<V>> entry : mergedFilters.entrySet()) {
			filterList.addAll(entry.getValue());
		}
		for (int i = filterList.size() - 1; i >= 0; i--) {
			final V filter = filterList.get(i);
			final ServiceInvocationHandler next = last;
			last = new ServiceInvocationHandler() {
				@SuppressWarnings("unchecked")
				@Override
				public InvocationResponse handle(InvocationContext invocationContext) throws Throwable {
					return filter.invoke(next, invocationContext);
				}
			};
		}
		return last;
	}

	public static void registerBizProcessFilter(ProcessPhase phase, ServiceInvocationFilter<ProviderContext> filter) {
		if (logger.isInfoEnabled()) {
			logger.info("register process filter, name:" + phase.name() + " the class name is:" + filter.getClass());
		}
		List<ServiceInvocationFilter<ProviderContext>> filters = bizProcessFilters.get(phase);
		if (filters == null) {
			filters = new ArrayList<ServiceInvocationFilter<ProviderContext>>();
			bizProcessFilters.put(phase, filters);
		}
		filters.add(filter);
	}

	public static void registerHeartBeatProcessFilter(ProcessPhase phase,
			ServiceInvocationFilter<ProviderContext> filter) {
		if (logger.isInfoEnabled()) {
			logger.info("register heartbeat filter, name:" + phase.name() + " the class name is:" + filter.getClass());
		}
		List<ServiceInvocationFilter<ProviderContext>> filters = heartBeatProcessFilters.get(phase);
		if (filters == null) {
			filters = new ArrayList<ServiceInvocationFilter<ProviderContext>>();
			heartBeatProcessFilters.put(phase, filters);
		}
		filters.add(filter);
	}

	private static void registerEchoProcessFilter(ProcessPhase phase, ServiceInvocationFilter<ProviderContext> filter) {
		if (logger.isInfoEnabled()) {
			logger.info("register echo filter, name:" + phase.name() + " the class name is:" + filter.getClass());
		}
		List<ServiceInvocationFilter<ProviderContext>> filters = echoProcessFilters.get(phase);
		if (filters == null) {
			filters = new ArrayList<ServiceInvocationFilter<ProviderContext>>();
			echoProcessFilters.put(phase, filters);
		}
		filters.add(filter);
	}

	public static void clearServerInternalFilters() {
		bizProcessFilters.clear();
		heartBeatProcessFilters.clear();
	}

}
