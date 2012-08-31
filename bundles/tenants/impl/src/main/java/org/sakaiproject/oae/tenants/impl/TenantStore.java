package org.sakaiproject.oae.tenants.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.oae.jaxrs.api.JaxrsService;
import org.sakaiproject.oae.tenants.api.OaeWebContext;
import org.sakaiproject.oae.tenants.api.Tenant;
import org.sakaiproject.oae.tenants.api.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/tenants")
@Produces(MediaType.APPLICATION_JSON)
@Service(value = JaxrsService.class)
@Component(metatype = true)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class TenantStore implements JaxrsService {

  @Reference
  protected OaeWebContext webContext;
  
	@Reference
	protected TenantService tenantService;
	
	protected static final Logger LOGGER = LoggerFactory
			.getLogger(TenantStore.class);

	@GET
	@Path("/id/{id}")
	public Tenant getById(@PathParam("id") int id) {
		LOGGER.info("Getting tenant.");
		return tenantService.getTenantById(id);
	}

	@GET
	@Path("/test")
	public Tenant test() {
		return new Tenant(0, 0, "sdfsdfsdf!");
	}

	@GET
	@Path("/current")
	public Tenant current(@Context HttpServletRequest servletRequest) {
		return getById(webContext.getTenantId());
	}

	@GET
	@Path("/list")
	public Collection<Tenant> getAllTenants() {
		return tenantService.getAllTenants();
	}

	@POST
	@Path("/add")
	public Tenant addTenant(@FormParam("name") String name, @FormParam("port") Integer port) {
		if (port == null)
			return tenantService.createTenant(name);
		else
			return tenantService.createTenant(port, name);
	}

}
