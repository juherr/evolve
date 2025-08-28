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
import lombok.Getter;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.rewrite.handler.RedirectPatternRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import jakarta.servlet.DispatcherType;

/**
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 07.04.2015
 */
@Getter
public class SteveAppContext {

    private final String location;
    private final String contextPath;
    private final boolean isGzipEnabled;
    private final WebAppContext webAppContext;

    public SteveAppContext(SteveConfiguration config, LogFileRetriever logFileRetriever, EndpointInfo info) {
        this.location = config.getPaths().getLocation();
        this.contextPath = config.getPaths().getContextPath();
        this.isGzipEnabled = config.getJetty().isGzipEnabled();

        this.webAppContext = createWebApp(config, logFileRetriever, info);
    }

    private static WebApplicationContext createSpringContext(
            ApplicationContext parent,
            SteveConfiguration config,
            LogFileRetriever logFileRetriever,
            EndpointInfo info) {
        var springContext = new AnnotationConfigServletWebApplicationContext();
        springContext.setParent(parent);
        springContext.scan("de.rwth.idsg.steve.config");
        springContext.registerBean(SteveConfiguration.class, () -> config);
        springContext.registerBean(LogFileRetriever.class, () -> logFileRetriever);
        springContext.registerBean(EndpointInfo.class, () -> info);

        return springContext;
    }

    private static WebAppContext createWebApp(
            SteveConfiguration config, LogFileRetriever logFileRetriever, EndpointInfo info) {
        var ctx = new WebAppContext();
        ctx.setContextPath(config.getPaths().getContextPath());

        // if during startup an exception happens, do not swallow it, throw it
        ctx.setThrowUnavailableOnStartupException(true);

        // Disable directory listings if no index.html is found.
        ctx.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");

        var rootContext = new AnnotationConfigApplicationContext();
        rootContext.scan("de.rwth.idsg.steve.ui.config");
        rootContext.refresh();
        var jettyCustomizer = rootContext.getBean(JettyCustomizer.class);
        jettyCustomizer.configure(ctx);

        var springContext = createSpringContext(rootContext, config, logFileRetriever, info);
        ctx.addEventListener(new ContextLoaderListener(springContext));

        var web = new ServletHolder("spring-dispatcher", new DispatcherServlet(springContext));
        ctx.addServlet(web, config.getPaths().getRootMapping());

        var cxf = new ServletHolder("cxf", new CXFServlet());
        ctx.addServlet(cxf, config.getPaths().getSoapMapping() + "/*");

        // add spring security
        ctx.addFilter(
                // The bean name is not arbitrary, but is as expected by Spring
                new FilterHolder(
                        new DelegatingFilterProxy(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME)),
                config.getPaths().getRootMapping() + "*",
                EnumSet.allOf(DispatcherType.class));

        return ctx;
    }

    public ContextHandlerCollection createHandlers() {
        var redirectContextHandler = createRedirectContextHandler(contextPath, location);
        var webAppContextHandler = createWebAppContextHandler(webAppContext, isGzipEnabled);
        return new ContextHandlerCollection(redirectContextHandler, webAppContextHandler);
    }

    private static ContextHandler createWebAppContextHandler(Handler handler, boolean isGzipEnabled) {
        // Wraps the whole web app in a gzip handler to make Jetty return compressed content
        // http://www.eclipse.org/jetty/documentation/current/gzip-filter.html
        return new ContextHandler(isGzipEnabled ? new GzipHandler(handler) : handler);
    }

    private static ContextHandler createRedirectContextHandler(String contextPath, String location) {
        var rewrite = new RewriteHandler();
        for (var redirect : getRedirectSet(contextPath)) {
            var rule = new RedirectPatternRule();
            rule.setTerminating(true);
            rule.setPattern(redirect);
            rule.setLocation(location);
            rewrite.addRule(rule);
        }
        return new ContextHandler(rewrite);
    }

    private static Set<String> getRedirectSet(String path) {
        var redirectSet = HashSet.<String>newHashSet(3);
        redirectSet.add("");
        redirectSet.add(path);

        // Otherwise (if path = ""), we would already be at root of the server ("/")
        // and using the redirection below would cause an infinite loop.
        if (!"".equals(path)) {
            redirectSet.add(path + "/");
        }

        return redirectSet;
    }
}
