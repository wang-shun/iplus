package com.sogou.iplus.config.servlet;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration.Dynamic;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import com.sogou.iplus.config.*;

public class WebInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

  @Override
  protected Class<?>[] getRootConfigClasses() {
    return new Class<?>[] { RootConfig.class, DaoConfig.class, SecurityConfig.class };
  }

  @Override
  protected Class<?>[] getServletConfigClasses() {
    return new Class<?>[] { WebConfig.class, BuildInApiConfig.class };
  }

  @Override
  protected Filter[] getServletFilters() {
    return new Filter[] { new DelegatingFilterProxy("loggerFilter"), new DelegatingFilterProxy("xssFilter"), };
  }

  @Override
  protected String[] getServletMappings() {
    return new String[] { "/" };
  }

  @Override
  protected void customizeRegistration(Dynamic registration) {
    registration.setMultipartConfig(new MultipartConfigElement("/tmp", 2500_000, 2500_000, 2500_000));
  }
}
