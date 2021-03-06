package com.sogou.iplus.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import commons.spring.*;
import commons.saas.PermService;
import commons.saas.RestNameService;
import commons.saas.XiaopLoginService;
import commons.saas.XiaopService;

@Configuration
@EnableScheduling
@ComponentScan({ ProjectInfo.PKG_PREFIX + ".api", ProjectInfo.PKG_PREFIX + ".manager" })
@PropertySource(value = "classpath:application-default.properties", ignoreResourceNotFound = true)
@PropertySource("classpath:application-${spring.profiles.active}.properties")
public class RootConfig {

  @Autowired
  Environment env;

  @Bean
  public MBeanServerFactoryBean mbeanServer() {
    MBeanServerFactoryBean mbeanServer = new MBeanServerFactoryBean();
    mbeanServer.setDefaultDomain(ProjectInfo.PKG_PREFIX);
    mbeanServer.setLocateExistingServerIfPossible(true);
    return mbeanServer;
  }

  @Bean
  public AnnotationMBeanExporter annotationMBeanExporter() {
    return new AnnotationMBeanExporter();
  }

  @Bean
  public LoggerFilter loggerFilter() {
    return new LoggerFilter(env);
  }

  @Bean
  public XssFilter xssFilter() {
    return new XssFilter(env);
  }

  @Bean
  public JedisPool jedisPool() {
    return new JedisPool(new JedisPoolConfig(), env.getRequiredProperty("redis.url"),
        env.getRequiredProperty("redis.port", Integer.class), 200, // default timeout 2000 millisecond is too long, set to 200
        env.getProperty("redis.pass")); // if null, ignore pass
  }

  @Bean
  public RedisRememberMeService rememberMeServices() {
    RedisRememberMeService rms = new RedisRememberMeService(jedisPool(), env.getProperty("rest.tokenpool", ""),
        env.getProperty("rest.inner", Boolean.class, false), env.getProperty("rest.anonymous", ""));
    rms.setCookiePrefix(env.getProperty("login.cookieprefix", ""));
    return rms;
  }

  @Bean
  public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(Integer.parseInt(env.getProperty("threadpool.max", "1")));
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    return scheduler;
  }

  @Bean
  public RestNameService restNameService() {
    return new RestNameService(env);
  }

  @Bean
  public RestTemplate restTemplate() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(Integer.parseInt(env.getProperty("rest.timeout.connect", "1000")));
    factory.setReadTimeout(Integer.parseInt(env.getProperty("rest.timeout.read", "10000")));

    RestTemplate rest = new RestTemplate(factory);
    rest.setInterceptors(Arrays.asList(new RestTemplateFilter()));
    rest.getMessageConverters().add(new LooseGsonHttpMessageConverter());

    return rest;
  }

  @Bean
  public XiaopLoginService xiaopLoginService() {
    return new XiaopLoginService(jedisPool(), restTemplate());
  }

  @Bean
  public XiaopService pandoraService() {
    XiaopService service = new XiaopService(restTemplate());
    service.setAppId(env.getRequiredProperty("pandora.message.publicid"));
    service.setAppKey(env.getRequiredProperty("pandora.message.token"));
    return service;
  }

  @Bean
  public PermService permService() {
    return new PermService(restTemplate(), restNameService());
  }

}
