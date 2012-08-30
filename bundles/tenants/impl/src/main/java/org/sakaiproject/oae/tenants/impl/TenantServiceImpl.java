package org.sakaiproject.oae.tenants.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.oae.tenants.api.Tenant;
import org.sakaiproject.oae.tenants.api.TenantService;

@Service
@Component(metatype = true)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class TenantServiceImpl implements TenantService {

	private List<Tenant> tenants = new ArrayList<Tenant>();

	@Override
	public Tenant getCurrentTenant() {
		// TODO Use ThreadLocal.
		return null;
	}

	@Override
	public Tenant createTenant(String tenantName) {
		// TODO persist etc..
		int id = tenants.size() + 1;
		int port = 8000 + id;
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
