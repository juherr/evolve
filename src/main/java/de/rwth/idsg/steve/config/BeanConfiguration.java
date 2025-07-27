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
package de.rwth.idsg.steve.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mysql.cj.conf.PropertyKey;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.rwth.idsg.steve.SteveConfiguration;
import de.rwth.idsg.steve.service.DummyReleaseCheckService;
import de.rwth.idsg.steve.service.GithubReleaseCheckService;
import de.rwth.idsg.steve.service.ReleaseCheckService;
import de.rwth.idsg.steve.utils.InternetChecker;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import jakarta.validation.Validator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

import javax.sql.DataSource;
import java.util.List;
import java.util.Properties;

/**
 * Configuration and beans of Spring Framework.
 *
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 15.08.2014
 */
@Slf4j
@Configuration
@EnableWebMvc
@EnableScheduling
@ComponentScan("de.rwth.idsg.steve")
public class BeanConfiguration implements WebMvcConfigurer, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
     */
    @Bean
    public DataSource dataSource() {
        SteveConfiguration config = steveConfiguration();
        SteveConfiguration.DB dbConfig = config.getDb();
        var dbUrl = "jdbc:mysql://" + dbConfig.getIp() + ":" + dbConfig.getPort() + "/" + dbConfig.getSchema();
        return dataSource(dbUrl, dbConfig.getUserName(), dbConfig.getPassword(), config.getTimeZoneId());
    }

    public DataSource dataSource(String dbUrl, String dbUserName, String dbPassword, String dbTimeZoneId) {
        HikariConfig hc = new HikariConfig();

        // set standard params
        hc.setJdbcUrl(dbUrl);
        hc.setUsername(dbUserName);
        hc.setPassword(dbPassword);

        // set non-standard params
        hc.addDataSourceProperty(PropertyKey.cachePrepStmts.getKeyName(), true);
        hc.addDataSourceProperty(PropertyKey.useServerPrepStmts.getKeyName(), true);
        hc.addDataSourceProperty(PropertyKey.prepStmtCacheSize.getKeyName(), 250);
        hc.addDataSourceProperty(PropertyKey.prepStmtCacheSqlLimit.getKeyName(), 2048);
        hc.addDataSourceProperty(PropertyKey.characterEncoding.getKeyName(), "utf8");
        hc.addDataSourceProperty(PropertyKey.connectionTimeZone.getKeyName(), dbTimeZoneId);
        hc.addDataSourceProperty(PropertyKey.useSSL.getKeyName(), true);

        // https://github.com/steve-community/steve/issues/736
        hc.setMaxLifetime(580_000);

        return new HikariDataSource(hc);
    }

    /**
     * Can we re-use DSLContext as a Spring bean (singleton)? Yes, the Spring tutorial of
     * Jooq also does it that way, but only if we do not change anything about the
     * config after the init (which we don't do anyways) and if the ConnectionProvider
     * does not store any shared state (we use DataSourceConnectionProvider of Jooq, so no problem).
     *
     * Some sources and discussion:
     * - http://www.jooq.org/doc/3.6/manual/getting-started/tutorials/jooq-with-spring/
     * - http://jooq-user.narkive.com/2fvuLodn/dslcontext-and-threads
     * - https://groups.google.com/forum/#!topic/jooq-user/VK7KQcjj3Co
     * - http://stackoverflow.com/questions/32848865/jooq-dslcontext-correct-autowiring-with-spring
     */
    @Bean
    public DSLContext dslContext(DataSource dataSource) {
        Settings settings = new Settings()
                // Normally, the records are "attached" to the Configuration that created (i.e. fetch/insert) them.
                // This means that they hold an internal reference to the same database connection that was used.
                // The idea behind this is to make CRUD easier for potential subsequent store/refresh/delete
                // operations. We do not use or need that.
                .withAttachRecords(false)
                // To log or not to log the sql queries, that is the question
                .withExecuteLogging(steveConfiguration().getDb().isSqlLogging());

        // Configuration for JOOQ
        org.jooq.Configuration conf = new DefaultConfiguration()
                .set(SQLDialect.MYSQL)
                .set(new DataSourceConnectionProvider(dataSource))
                .set(settings);

        return DSL.using(conf);
    }

    @Bean(destroyMethod = "close")
    public DelegatingTaskScheduler asyncTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("SteVe-TaskScheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();

        return new DelegatingTaskScheduler(scheduler);
    }

    @Bean(destroyMethod = "close")
    public DelegatingTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setThreadNamePrefix("SteVe-TaskExecutor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        return new DelegatingTaskExecutor(executor);
    }

    @Bean
    public Validator validator() {
        return new LocalValidatorFactoryBean();
    }

    /**
     * There might be instances deployed in a local/closed network with no internet connection. In such situations,
     * it is unnecessary to try to access Github every time, even though the request will time out and result
     * report will be correct (that there is no new version). With DummyReleaseCheckService we bypass the intermediate
     * steps and return a "no new version" report immediately.
     */
    @Bean
    public ReleaseCheckService releaseCheckService() {
        if (InternetChecker.isInternetAvailable()) {
            return new GithubReleaseCheckService(steveConfiguration());
        } else {
            return new DummyReleaseCheckService();
        }
    }

    // -------------------------------------------------------------------------
    // Web config
    // -------------------------------------------------------------------------

    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(this.applicationContext);
        templateResolver.setPrefix("classpath:/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        // Template cache is true by default. Set to false if you want
        // templates to be automatically updated when modified.
        templateResolver.setCacheable(true);
        return templateResolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }

    @Bean
    public ThymeleafViewResolver viewResolver() {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        return viewResolver;
    }

    /**
     * Resource path for static content of the Web interface.
     */
    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600)
                .resourceChain(true);

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .setCachePeriod(3600)
                .resourceChain(true);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/manager/signin").setViewName("signin");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.mediaType("svg", MediaType.valueOf("image/svg+xml"));
        configurer.mediaType("png", MediaType.IMAGE_PNG);
        configurer.mediaType("gif", MediaType.IMAGE_GIF);
        configurer.mediaType("jpg", MediaType.IMAGE_JPEG);
        configurer.mediaType("ico", MediaType.valueOf("image/x-icon"));
        configurer.mediaType("js", MediaType.valueOf("text/javascript"));
        configurer.mediaType("css", MediaType.valueOf("text/css"));
        configurer.mediaType("ttf", MediaType.valueOf("font/ttf"));
    }

    // -------------------------------------------------------------------------
    // API config
    // -------------------------------------------------------------------------

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter conv = (MappingJackson2HttpMessageConverter) converter;
                ObjectMapper objectMapper = conv.getObjectMapper();
                objectMapper.findAndRegisterModules();
                // if the client sends unknown props, just ignore them instead of failing
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                // default is true
                objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                break;
            }
        }
    }

    /**
     * Find the ObjectMapper used in MappingJackson2HttpMessageConverter and initialized by Spring automatically.
     * MappingJackson2HttpMessageConverter is not a Bean. It is created in {@link WebMvcConfigurationSupport#addDefaultHttpMessageConverters(List)}.
     * Therefore, we have to access it via proxies that reference it. RequestMappingHandlerAdapter is a Bean, created in
     * {@link WebMvcConfigurationSupport#requestMappingHandlerAdapter(ContentNegotiationManager, FormattingConversionService, org.springframework.validation.Validator)}.
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
        return requestMappingHandlerAdapter.getMessageConverters().stream()
            .filter(converter -> converter instanceof MappingJackson2HttpMessageConverter)
            .findAny()
            .map(conv -> ((MappingJackson2HttpMessageConverter) conv).getObjectMapper())
            .orElseThrow(() -> new RuntimeException("There is no MappingJackson2HttpMessageConverter in Spring context"));
    }

    @Bean
    public SteveConfiguration steveConfiguration() {
        return SteveConfiguration.CONFIG;
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer valueConfigurer() {
        var configurer = new PropertySourcesPlaceholderConfigurer();

        var props = new Properties();
        var chargeBoxIdValidationRegex = steveConfiguration().getOcpp().getChargeBoxIdValidationRegex();
        if (chargeBoxIdValidationRegex != null) {
          props.put("charge-box-id.validation.regex", chargeBoxIdValidationRegex);
        }
        configurer.setProperties(props);

        return configurer;
    }
}
