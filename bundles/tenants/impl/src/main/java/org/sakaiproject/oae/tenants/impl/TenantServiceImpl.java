package org.sakaiproject.oae.tenants.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.oae.tenants.api.OaeWebContext;
import org.sakaiproject.oae.tenants.api.Tenant;
import org.sakaiproject.oae.tenants.api.TenantService;

@Service
@Component(metatype = true)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class TenantServiceImpl implements TenantService {

	@Reference
	protected OaeWebContext webContext;

	private List<Tenant> tenants = new ArrayList<Tenant>();

	@SuppressWarnings("unused")
	@Activate
	@Modified
	private void activate(Map<Object, Object> properties) {
		tenants.add(new Tenant(1, 8080, "Cambridge University"));
		tenants.add(new Tenant(2, 8081, "New York University"));
		tenants.add(new Tenant(3, 8082, "Georgia Tech"));
		tenants.add(new Tenant(4, 8083, "AAR"));
		tenants.add(new Tenant(5, 8084, "CSU"));
	}

	@Override
	public Tenant getCurrentTenant(ServletRequest req) {
		//int port = webContext.getPort();
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
