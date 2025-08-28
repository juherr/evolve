/*
 * SteVe - SteckdosenVerwaltung - https://github.com/steve-community/steve
 * Copyright (C) 2013-2025 SteVe Community Team
 * All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.rwth.idsg.steve;

import de.rwth.idsg.steve.utils.LogFileRetriever;
import de.rwth.idsg.steve.web.dto.EndpointInfo;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.websocket.core.WebSocketConstants;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import static de.rwth.idsg.steve.config.OcppWebSocketConfiguration.WS_IDLE_TIMEOUT;
import static de.rwth.idsg.steve.config.OcppWebSocketConfiguration.WS_MAX_MSG_SIZE;

/**
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 12.12.2014
 */
@Slf4j
public class JettyServer extends Server {

    private static final int MIN_THREADS = 4;
    private static final int MAX_THREADS = 50;

    private static final long STOP_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
    private static final long IDLE_TIMEOUT = TimeUnit.MINUTES.toMillis(1);

    private final SteveConfiguration config;
    private final LogFileRetriever logFileRetriever;

    public JettyServer(SteveConfiguration config, LogFileRetriever logFileRetriever) {
        super(createThreadPool());
        this.config = config;
        this.logFileRetriever = logFileRetriever;
        this.addEventListener(updateWsDefault());
    }

    private static QueuedThreadPool createThreadPool() {
        // === jetty.xml ===
        var threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(MIN_THREADS);
        threadPool.setMaxThreads(MAX_THREADS);
        return threadPool;
    }

    /**
     * A fully configured Jetty Server instance
     */
    @Override
    public void doStart() throws Exception {
        // Scheduler
        this.addBean(new ScheduledExecutorScheduler());

        // HTTP Configuration
        var httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme(HttpScheme.HTTPS.asString());
        httpConfig.setSecurePort(config.getJetty().getHttpsPort());
        httpConfig.setOutputBufferSize(32768);
        httpConfig.setRequestHeaderSize(8192);
        httpConfig.setResponseHeaderSize(8192);
        httpConfig.setSendServerVersion(false);
        httpConfig.setSendDateHeader(false);
        httpConfig.setSendXPoweredBy(false);

        // make sure X-Forwarded-For headers are picked up if set (e.g. by a load balancer)
        // https://github.com/steve-community/steve/pull/570
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());

        // Extra options
        this.setDumpAfterStart(false);
        this.setDumpBeforeStop(false);
        this.setStopAtShutdown(true);
        this.setStopTimeout(STOP_TIMEOUT);

        if (config.getJetty().isHttpEnabled()) {
            this.addConnector(createHttpConnector(
                    this, config.getJetty().getServerHost(), config.getJetty().getHttpPort(), httpConfig));
        }

        if (config.getJetty().isHttpsEnabled()) {
            this.addConnector(createHttpsConnector(
                    this,
                    config.getJetty().getServerHost(),
                    config.getJetty().getHttpPort(),
                    httpConfig,
                    config.getJetty().getKeyStorePath(),
                    config.getJetty().getKeyStorePassword()));
        }

        var info = new EndpointInfo(config);
        populateInfo(info);

        var steveAppContext = new SteveAppContext(config, logFileRetriever, info);
        this.setHandler(steveAppContext.createHandlers());

        super.doStart();
    }

    private void populateInfo(EndpointInfo info) {
        var list = Arrays.stream(getConnectors())
                .map(c -> getConnectorPath(
                        config.getJetty().getServerHost(), config.getPaths().getContextPath(), c))
                .flatMap(ips -> ips.entrySet().stream())
                .toList();
        setList(
                list,
                isSecured -> Boolean.TRUE.equals(isSecured) ? "https" : "http",
                info.getWebInterface(),
                info.getOcppSoap());
        setList(list, isSecured -> Boolean.TRUE.equals(isSecured) ? "wss" : "ws", info.getOcppWebSocket());
    }

    private static ServerConnector createHttpConnector(
            Server server, String host, int port, HttpConfiguration httpconfig) {
        // === jetty-http.xml ===
        var http = new ServerConnector(server, new HttpConnectionFactory(httpconfig));
        http.setHost(host);
        http.setPort(port);
        http.setIdleTimeout(IDLE_TIMEOUT);
        return http;
    }

    private ServerConnector createHttpsConnector(
            Server server,
            String host,
            int port,
            HttpConfiguration httpConfig,
            String keyStorePath,
            String keyStorePassword) {
        // === jetty-https.xml ===
        // SSL Context Factory
        var sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(keyStorePath);
        sslContextFactory.setKeyStorePassword(keyStorePassword);
        sslContextFactory.setKeyManagerPassword(keyStorePassword);

        // SSL HTTP Configuration
        var httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        // SSL Connector
        var https = new ServerConnector(
                server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(httpsConfig));
        https.setHost(host);
        https.setPort(port);
        https.setIdleTimeout(IDLE_TIMEOUT);
        return https;
    }

    /**
     * Otherwise, defaults come from {@link WebSocketConstants}
     */
    private static ServletContextListener updateWsDefault() {
        return new ServletContextListener() {
            @Override
            public void contextInitialized(ServletContextEvent sce) {
                var container = JettyWebSocketServerContainer.getContainer(sce.getServletContext());
                container.setMaxTextMessageSize(WS_MAX_MSG_SIZE);
                container.setIdleTimeout(WS_IDLE_TIMEOUT);
            }
        };
    }

    private static Map<String, Boolean> getConnectorPath(String serverHost, String contextPath, Connector c) {
        var sc = (ServerConnector) c;

        var isSecure = sc.getDefaultConnectionFactory() instanceof SslConnectionFactory;
        var layout = "://%s:%d" + contextPath;

        return getIps(sc, serverHost).stream()
                .map(k -> String.format(layout, k, sc.getPort()))
                .collect(HashMap::new, (m, v) -> m.put(v, isSecure), HashMap::putAll);
    }

    private static Set<String> getIps(ServerConnector sc, String serverHost) {
        var ips = new HashSet<String>();
        var host = sc.getHost();
        if (host == null || host.equals("0.0.0.0")) {
            ips.addAll(getPossibleIpAddresses(serverHost));
        } else {
            ips.add(host);
        }
        return ips;
    }

    private static void setList(
            List<Map.Entry<String, Boolean>> list,
            Function<Boolean, String> prefix,
            EndpointInfo.ItemsWithInfo... items) {
        var ws = list.stream().map(e -> prefix.apply(e.getValue()) + e.getKey()).toList();
        for (EndpointInfo.ItemsWithInfo item : items) {
            item.setData(ws);
        }
    }

    /**
     * Uses different APIs to find out the IP of this machine.
     */
    private static List<String> getPossibleIpAddresses(String serverHost) {
        var host = "treibhaus.informatik.rwth-aachen.de";
        var ips = new ArrayList<String>();

        try {
            ips.add(InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            // fail silently
        }

        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, 80));
            ips.add(socket.getLocalAddress().getHostAddress());
        } catch (Exception e) {
            // fail silently
        }

        // https://stackoverflow.com/a/38342964
        try (var socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ips.add(socket.getLocalAddress().getHostAddress());
        } catch (Exception e) {
            // fail silently
        }

        // https://stackoverflow.com/a/20418809
        try {
            for (var ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                var iface = ifaces.nextElement();
                for (var inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    var inetAddr = inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress() && (inetAddr instanceof Inet4Address)) {
                        ips.add(inetAddr.getHostAddress());
                    }
                }
            }

        } catch (Exception e) {
            // fail silently
        }

        ips.removeIf("0.0.0.0"::equals);

        if (ips.isEmpty()) {
            // Well, we failed to read from system, fall back to main.properties.
            // Better than nothing
            ips.add(serverHost);
        }

        return ips;
    }
}
