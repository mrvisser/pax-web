package org.sakaiproject.oae.tenants.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.sakaiproject.oae.tenants.api.OaeWebContext;
import org.sakaiproject.oae.tenants.api.Tenant;
import org.sakaiproject.oae.tenants.api.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

@Component(immediate = true, metatype = true)
@Service({ OaeWebContext.class, Filter.class })
@Properties(value = {
		@Property(name = "service.description", value = "Runs the requested page trough a headless browser so we can display proper content to the Google Crawler."),
		@Property(name = "service.vendor", value = "The Sakai Foundation"),
		@Property(name = "urlPatterns", value = { "/" }),
		@Property(name = "filter-name", value = "tenant-filter") })
public class ThreadLocalOaeWebContext implements Filter, OaeWebContext {

	private final static Logger LOGGER = LoggerFactory
			.getLogger(ThreadLocalOaeWebContext.class);

	private final static ThreadLocal<BeanNakamuraWebContextImpl> webContextThreadLocal = new ThreadLocal<BeanNakamuraWebContextImpl>();

	@Reference
	protected TenantService tenantService;
	
	public ThreadLocalOaeWebContext(TenantService tenantService) {
	  this.tenantService = tenantService;
	}
	
	public ThreadLocalOaeWebContext() {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		LOGGER.info("init()");
	}

	@Activate
	@Modified
	public void activate(BundleContext bundleContext) {
		LOGGER.info("activate()");

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		initWebContext(request, response);
		try {
			chain.doFilter(request, response);
		} finally {
			destroyWebContext();
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see javax.servlet.Filter#destroy()
	 */
	@Override
	public void destroy() {
		LOGGER.info("destroy()");
	}

	/**
	 * Initialize the web context for the given request.
	 * 
	 * @param request
	 * @param response
	 */
	void initWebContext(ServletRequest request, ServletResponse response) {
		if (webContextThreadLocal.get() != null) {
			LOGGER.warn("A web context was initialized when one alread existed. This could indicate that "
					+ "a web context was left lingering on a thread, which is dangerous.");
			destroyWebContext();
		}
		BeanNakamuraWebContextImpl webContext = new BeanNakamuraWebContextImpl();
		resolveTenant(request, response, webContext);
		webContextThreadLocal.set(webContext);
	}

	/**
	 * Determines the current tenant's ID by looking at the port we're running
	 * on.
	 * 
	 * @param request
	 * @param response
	 * @param webContext
	 * @return
	 */
	private int resolveTenant(ServletRequest request, ServletResponse response,
			BeanNakamuraWebContextImpl webContext) {
		int port = request.getServerPort();
		Tenant tenant = tenantService.getTenantByPort(port);
		if (tenant == null) {
		  LOGGER.error("Access made for invalid tenant port: "+port);
		  throw new IllegalStateException("Not tenant configured for requested port");
		}
		webContext.setTenantId(tenant.getId());
		return tenant.getId();
	}

	/**
	 * Destroy the thread web context. This will release all resources and
	 * completely unbind the context from the thread.
	 */
	void destroyWebContext() {
		BeanNakamuraWebContextImpl webContext = webContextThreadLocal.get();

		if (webContext == null)
			return;

		// TODO: Once the OaeWebContext interface starts holding actual objects
		// we need to clear those as well.

		webContextThreadLocal.set(null);
	}

	/**
	 * Verify that the web context is initialized and in a state where it can be
	 * accessed for context data
	 */
	void validateInitialized() {
		if (webContextThreadLocal.get() == null)
			throw new IllegalStateException(
					"Attempted to access null web context.");

		// we can determine if this was initialized based on whether or not the
		// tenant id was set.
		if (webContextThreadLocal.get().getTenantId() == Integer.MIN_VALUE)
			throw new IllegalStateException(
					"Attempted to access uninitialized web context");
	}

	@Override
	public int getTenantId() throws IllegalStateException {
		return webContextThreadLocal.get().getTenantId();
	}

}
