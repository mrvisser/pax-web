package org.sakaiproject.oae.tenants.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.oae.tenants.api.Tenant;
import org.sakaiproject.oae.tenants.api.TenantService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;

@Service
@Component(metatype = true)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class TenantServiceImpl implements TenantService {

  private final static String DEFAULT_TENANT_PORT_MAPPING = "default:8080";
  
	@Property(label="Tenant-port mappings", description="A mapping of tenantId:port for all tenants in the system",
	    value={DEFAULT_TENANT_PORT_MAPPING}, unbounded=PropertyUnbounded.ARRAY)
	protected final static String PROP_TENANT_PORT_MAPPING = "tenant-port-mapping";
	
	private List<Tenant> tenants = new ArrayList<Tenant>();

	@SuppressWarnings("unused")
	@Activate
	@Modified
	private synchronized void activate(Map<Object, Object> properties) {
	  Object tenantMappingsObj = properties.get(PROP_TENANT_PORT_MAPPING);
	  
	  tenants.clear();
	  
	  String[] tenantMappings = (tenantMappingsObj instanceof String) ? new String[] {
	    (String) tenantMappingsObj } : (String[]) tenantMappingsObj; 
	  
	  if (tenantMappings == null)
	    tenantMappings = new String[] { DEFAULT_TENANT_PORT_MAPPING };
	  
	  int i = 0;
	  for (String tenantMapping : tenantMappings) {
	    String[] split = tenantMapping.split(":");
	    tenants.add(new Tenant(i, Integer.valueOf(split[1]), split[0]));
	    i++;
	  }
	}

	@Override
	public Tenant getCurrentTenant(ServletRequest req) {
		int port = req.getServerPort();
		return getTenantByPort(port);
	}

	@Override
	public Tenant createTenant(String tenantName) {
		// TODO persist etc..
		int id = tenants.size() + 1;
		int port = 8000 + id;
		return createTenant(id, port, tenantName);
	}

	@Override
	public Tenant createTenant(int port, String tenantName) {
		int id = tenants.size() + 1;
		return createTenant(id, port, tenantName);
	}

	public Tenant createTenant(int id, int port, String tenantName) {
		Tenant tenant = new Tenant(id, port, tenantName);
		tenants.add(tenant);
		return tenant;
	}

	@Override
	public Tenant getTenantByPort(int port) {
		for (Tenant tenant : tenants) {
			if (tenant.getPort() == port) {
				return tenant;
			}
		}
		return null;
	}

	@Override
	public Tenant getTenantById(int id) {
		for (Tenant tenant : tenants) {
			if (tenant.getId() == id) {
				return tenant;
			}
		}
		return null;
	}

	@Override
	public Collection<Tenant> getAllTenants() {
		return tenants;
	}
}
