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

import jakarta.servlet.DispatcherType;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.rewrite.handler.RedirectPatternRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.resource.URLResourceFactory;
import org.eclipse.jetty.websocket.core.WebSocketConstants;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import java.net.URL;
import java.util.EnumSet;
import java.util.HashSet;

import static de.rwth.idsg.steve.config.WebSocketConfiguration.IDLE_TIMEOUT;
import static de.rwth.idsg.steve.config.WebSocketConfiguration.MAX_MSG_SIZE;

/**
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 07.04.2015
 */
public class SteveAppContext {

    private final SteveConfiguration config;
    private final AnnotationConfigWebApplicationContext springContext;
    private final WebAppContext webAppContext;

    public SteveAppContext(SteveConfiguration config) {
        this.config = config;
        springContext = new AnnotationConfigWebApplicationContext();
        springContext.scan("de.rwth.idsg.steve.config");
        webAppContext = initWebApp();
    }

    public ContextHandlerCollection getHandlers() {
        return new ContextHandlerCollection(
            new ContextHandler(getRedirectHandler()),
            new ContextHandler(getWebApp())
        );
    }

    /**
     * Otherwise, defaults come from {@link WebSocketConstants}
     */
    public void configureWebSocket() {
        JettyWebSocketServerContainer container = JettyWebSocketServerContainer.getContainer(webAppContext.getServletContext());
        container.setMaxTextMessageSize(MAX_MSG_SIZE);
        container.setIdleTimeout(IDLE_TIMEOUT);
    }

    private Handler getWebApp() {
        if (!config.getJetty().isGzipEnabled()) {
            return webAppContext;
        }

        // Wraps the whole web app in a gzip handler to make Jetty return compressed content
        // http://www.eclipse.org/jetty/documentation/current/gzip-filter.html
        return new GzipHandler(webAppContext);
    }

    private WebAppContext initWebApp() {
        WebAppContext ctx = new WebAppContext();
        ctx.setContextPath(config.getContextPath());

        // if during startup an exception happens, do not swallow it, throw it
        ctx.setThrowUnavailableOnStartupException(true);

        // Disable directory listings if no index.html is found.
        ctx.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");

        // The location of the static resources, for Thymeleaf, etc.
        // Must be after the context is created.
        // For a JAR deployment, resources must be loaded from the classpath.
        URL staticResources = SteveAppContext.class.getClassLoader().getResource("static");
        if (staticResources != null) {
            var factory = new URLResourceFactory();
            ctx.setBaseResource(factory.newResource(staticResources));
        }

        ServletHolder web = new ServletHolder("spring-dispatcher", new DispatcherServlet(springContext));
        ServletHolder cxf = new ServletHolder("cxf", new CXFServlet());

        ctx.addEventListener(new ContextLoaderListener(springContext));
        ctx.addServlet(web, config.getSpringMapping());
        ctx.addServlet(cxf, config.getCxfMapping() + "/*");

        // add spring security
        ctx.addFilter(
            // The bean name is not arbitrary, but is as expected by Spring
            new FilterHolder(new DelegatingFilterProxy(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME)),
            config.getSpringMapping() + "*",
            EnumSet.allOf(DispatcherType.class)
        );

        return ctx;
    }

    private Handler getRedirectHandler() {
        RewriteHandler rewrite = new RewriteHandler();
        for (String redirect : getRedirectSet()) {
            RedirectPatternRule rule = new RedirectPatternRule();
            rule.setTerminating(true);
            rule.setPattern(redirect);
            rule.setLocation(config.getContextPath() + "/manager/home");
            rewrite.addRule(rule);
        }
        return rewrite;
    }

    private HashSet<String> getRedirectSet() {
        String path = config.getContextPath();

        HashSet<String> redirectSet = new HashSet<>(3);
        redirectSet.add("");
        redirectSet.add(path + "");

        // Otherwise (if path = ""), we would already be at root of the server ("/")
        // and using the redirection below would cause an infinite loop.
        if (!"".equals(path)) {
            redirectSet.add(path + "/");
        }

        return redirectSet;
    }
}
