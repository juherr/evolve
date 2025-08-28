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
package de.rwth.idsg.steve.ui.config;

import de.rwth.idsg.steve.JettyCustomizer;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.ee10.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.io.IOException;

@Configuration
public class JspConfiguration implements WebMvcConfigurer {

    /**
     * Resolver for JSP views/templates. Controller classes process the requests
     * and forward to JSP files for rendering.
     */
    @Bean
    public InternalResourceViewResolver urlBasedViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    /**
     * Resource path for static content of the Web interface.
     */
    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("static/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/manager/signin").setViewName("signin");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    /**
     * Help by:
     * https://github.com/jetty/jetty-examples/tree/12.0.x/embedded/ee10-jsp
     * https://github.com/jetty-project/embedded-jetty-jsp
     * https://github.com/jasonish/jetty-springmvc-jsp-template
     * http://examples.javacodegeeks.com/enterprise-java/jetty/jetty-jsp-example
     */
    @Bean
    public JettyCustomizer jettyCustomizer() {
        return ctx -> {
            ctx.setBaseResourceAsString(getWebAppURIAsString());
            ctx.addBean(new EmbeddedJspStarter(ctx));
            ctx.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        };
    }

    /**
     * From: https://github.com/jetty/jetty-examples/blob/12.0.x/embedded/ee10-jsp/src/main/java/examples/EmbeddedJspStarter.java
     *
     * JspStarter for embedded ServletContextHandlers
     *
     * This is added as a bean that is a jetty LifeCycle on the ServletContextHandler.
     * This bean's doStart method will be called as the ServletContextHandler starts,
     * and will call the ServletContainerInitializer for the jsp engine.
     */
    public static class EmbeddedJspStarter extends AbstractLifeCycle {

        private final JettyJasperInitializer sci;
        private final ServletContextHandler context;

        public EmbeddedJspStarter(ServletContextHandler context) {
            this.sci = new JettyJasperInitializer();
            this.context = context;

            // we dont need all this from the example, since our JSPs are precompiled
            //
            // StandardJarScanner jarScanner = new StandardJarScanner();
            // StandardJarScanFilter jarScanFilter = new StandardJarScanFilter();
            // jarScanFilter.setTldScan("taglibs-standard-impl-*");
            // jarScanFilter.setTldSkip("apache-*,ecj-*,jetty-*,asm-*,javax.servlet-*,javax.annotation-*,taglibs-standard-spec-*");
            // jarScanner.setJarScanFilter(jarScanFilter);
            // this.context.setAttribute("org.apache.tomcat.JarScanner", jarScanner);
        }

        @Override
        protected void doStart() throws Exception {
            var old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(context.getClassLoader());
            try {
                sci.onStartup(null, context.getServletContext());
                super.doStart();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
    }

    private static String getWebAppURIAsString() {
        try {
            return new ClassPathResource("webapp").getURI().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
