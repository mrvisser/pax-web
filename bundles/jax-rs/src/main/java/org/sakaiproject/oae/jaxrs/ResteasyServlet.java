/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.oae.jaxrs;

import com.google.common.collect.Sets;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.servlet.HttpRequestFactory;
import org.jboss.resteasy.plugins.server.servlet.HttpResponseFactory;
import org.jboss.resteasy.plugins.server.servlet.HttpServletInputMessage;
import org.jboss.resteasy.plugins.server.servlet.HttpServletResponseWrapper;
import org.jboss.resteasy.plugins.server.servlet.ServletBootstrap;
import org.jboss.resteasy.plugins.server.servlet.ServletContainerDispatcher;
import org.jboss.resteasy.specimpl.UriInfoImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.Registry;
import org.sakaiproject.oae.jaxrs.api.JaxrsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

/**
 * A servlet to enable JAX-RS services for the Nakamura container. This component takes responsibility
 * of collecting all JaxrsService's in the system and makes them available for the internal RestEasy
 * JAX-RS processing.
 */
@Component(immediate = true, metatype = true)
@Service(value = Servlet.class)
@References(value = {
    @Reference(name = "services", referenceInterface = JaxrsService.class,
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC,
        strategy = ReferenceStrategy.EVENT, bind = "bindService", unbind = "unbindService"
      )
  })
public class ResteasyServlet extends HttpServlet implements HttpRequestFactory,
    HttpResponseFactory {
  private static final long serialVersionUID = 3623498533852144726L;
  private static final Logger LOGGER = LoggerFactory.getLogger(ResteasyServlet.class);

  /**
   * The default top-level context for all JAX-RS services.
   */
  public static final String DEFAULT_ALIAS = "/api";

  @Property(value = DEFAULT_ALIAS, description = "The top-level context of the JAX-RS API space. Must begin with a '/'. "
      + "(Default: " + DEFAULT_ALIAS + ")", label = "Context")
  public static final String PROP_ALIAS = "alias";

  protected ServletContainerDispatcher servletContainerDispatcher;
  private Set<JaxrsService> pendingServices = Sets.newHashSet();
  private Object registrationSync = new Object();
  private String alias;

  public Dispatcher getDispatcher() {
    return servletContainerDispatcher.getDispatcher();
  }

  public Registry getRegistry() {
    return servletContainerDispatcher.getDispatcher().getRegistry();
  }

  @Activate
  public void activate(Map<String, Object> properties) {
    alias = (String) properties.get(PROP_ALIAS);
    alias = (alias == null) ? DEFAULT_ALIAS : alias;
    if (!alias.startsWith("/")) {
      LOGGER.warn("Alias parameter {} did not start with '/', prepending it automatically.",
          alias);
      alias = "/".concat(alias);
    }
  }

  @Deactivate
  public void deactivate(Map<String, Object> properties) {

  }

  @Override
  public void init(ServletConfig servletConfig) throws ServletException {
    synchronized (registrationSync) {
      ClassLoader bundleClassloader = this.getClass().getClassLoader();
      ClassLoader contextClassloader = Thread.currentThread().getContextClassLoader();
      try {
        Thread.currentThread().setContextClassLoader(bundleClassloader);
        super.init(servletConfig);
        ServletBootstrap bootstrap = new ServletBootstrap(servletConfig);
        servletContainerDispatcher = new ServletContainerDispatcher();
        servletContainerDispatcher.init(servletConfig.getServletContext(), bootstrap,
            this, this);
        servletContainerDispatcher.getDispatcher().getDefaultContextObjects()
            .put(ServletConfig.class, servletConfig);
      } finally {
        Thread.currentThread().setContextClassLoader(contextClassloader);
      }

      Registry registry = getRegistry();
      for (JaxrsService service : pendingServices) {
        LOGGER.info("Registering JaxRestService {} ", service);
        registry.addSingletonResource(service);
      }

      JacksonJaxbJsonProvider provider = createJsonProvider();
      servletContainerDispatcher.getDispatcher().getProviderFactory()
          .registerProviderInstance(provider);

      pendingServices.clear();
    }

  }

  @Override
  public void destroy() {
    synchronized (registrationSync) {
      super.destroy();
      LOGGER.info("Removing all JaxRestServices ");
      servletContainerDispatcher.destroy();
      servletContainerDispatcher = null;
    }
  }

  public void bindService(JaxrsService service) {
    synchronized (registrationSync) {
      if (servletContainerDispatcher == null) {
        pendingServices.add(service);
      } else {
        LOGGER.info("Registering JaxRestService {} ", service);
        getRegistry().addSingletonResource(service);
      }
    }
  }

  public void unbindService(JaxrsService service) {
    synchronized (registrationSync) {
      if (servletContainerDispatcher == null) {
        pendingServices.remove(service);
      } else {
        LOGGER.info("Removing JaxRestService {} ", service);
        getRegistry().removeRegistrations(service.getClass());
      }
    }
  }

  @Override
  protected void service(HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse) throws ServletException, IOException {
    service(httpServletRequest.getMethod(), httpServletRequest, httpServletResponse);
  }

  //@Profiled(tag="jaxrs:slowrequest:{$1.requestURI}", el=true, timeThreshold=1000)  
  public void service(String httpMethod, HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    servletContainerDispatcher.service(httpMethod, request, response, true);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.jboss.resteasy.plugins.server.servlet.HttpRequestFactory#createResteasyHttpRequest(java.lang.String,
   *      javax.servlet.http.HttpServletRequest, javax.ws.rs.core.HttpHeaders,
   *      org.jboss.resteasy.specimpl.UriInfoImpl, org.jboss.resteasy.spi.HttpResponse,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  public HttpRequest createResteasyHttpRequest(String httpMethod,
      HttpServletRequest request, HttpHeaders headers, UriInfoImpl uriInfo,
      HttpResponse theResponse, HttpServletResponse response) {
    uriInfo.pushMatchedURI(alias, alias);
    return createHttpRequest(httpMethod, request, headers, uriInfo, theResponse, response);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.jboss.resteasy.plugins.server.servlet.HttpResponseFactory#createResteasyHttpResponse(javax.servlet.http.HttpServletResponse)
   */
  @Override
  public HttpResponse createResteasyHttpResponse(HttpServletResponse response) {
    return createServletResponse(response);
  }

  protected HttpRequest createHttpRequest(String httpMethod, HttpServletRequest request,
      HttpHeaders headers, UriInfoImpl uriInfo, HttpResponse theResponse,
      HttpServletResponse response) {
    return new HttpServletInputMessage(request, theResponse, headers, uriInfo,
        httpMethod.toUpperCase(), (SynchronousDispatcher) getDispatcher());
  }

  protected HttpResponse createServletResponse(HttpServletResponse response) {
    return new HttpServletResponseWrapper(response, getDispatcher().getProviderFactory());
  }

  private JacksonJaxbJsonProvider createJsonProvider() {
    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospector primary = new JaxbAnnotationIntrospector();
    AnnotationIntrospector secondary = new JacksonAnnotationIntrospector();
    AnnotationIntrospector pair = new AnnotationIntrospector.Pair(primary, secondary);
    mapper.setAnnotationIntrospector(pair);

    // Set up the JAX-RS provider
    JacksonJaxbJsonProvider jaxbProvider = new JacksonJaxbJsonProvider();
    jaxbProvider.setMapper(mapper);
    return jaxbProvider;
  }

}
