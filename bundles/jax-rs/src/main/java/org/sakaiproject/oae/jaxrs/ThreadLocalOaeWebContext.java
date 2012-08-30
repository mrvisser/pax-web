package org.sakaiproject.oae.jaxrs;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.oae.jaxrs.api.OaeWebContext;
import org.sakaiproject.oae.tenants.api.Tenant;
import org.sakaiproject.oae.tenants.api.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service({ OaeWebContext.class, Filter.class })
@Property(name = "pattern", value = ".*")
public class ThreadLocalOaeWebContext implements OaeWebContext, Filter {

	private final static Logger LOGGER = LoggerFactory
			.getLogger(ThreadLocalOaeWebContext.class);

	@Reference
	private TenantService tenantService;

	private final static ThreadLocal<BeanNakamuraWebContextImpl> webContextThreadLocal = new ThreadLocal<BeanNakamuraWebContextImpl>();

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
	private Tenant resolveTenant(ServletRequest request,
			ServletResponse response, BeanNakamuraWebContextImpl webContext) {
		Tenant tenant = tenantService.getTenantByPort(request.getServerPort());
		if (tenant == null) {
			LOGGER.error("Can't get the tenant via the port! This should not happen.");
		}
		webContext.setTenant(tenant);
		return tenant;
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
		if (webContextThreadLocal.get().getTenant() == null)
			throw new IllegalStateException(
					"Attempted to access uninitialized web context");
	}

	@Override
	public Tenant getTenant() throws IllegalStateException {
		return webContextThreadLocal.get().getTenant();
	}

}
