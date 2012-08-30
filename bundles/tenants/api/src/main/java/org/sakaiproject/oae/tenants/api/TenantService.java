package org.sakaiproject.oae.tenants.api;

import java.util.Collection;

import javax.servlet.ServletRequest;

public interface TenantService {

	/**
	 * @return The current tenant.
	 */
	public Tenant getCurrentTenant(ServletRequest req);

	/**
	 * Each tenant runs on it's own port which can be used to determine it's ID.
	 * 
	 * @param port
	 * @return The tenant associated with that port.
	 */
	public Tenant getTenantByPort(int port);

	/**
	 * 
	 * @param id
	 * @return
	 */
	public Tenant getTenantById(int id);

	/**
	 * @return A list of all the tenants that are running on this sytem.
	 */
	public Collection<Tenant> getAllTenants();

	/**
	 * Creates a new tenant.
	 * 
	 * @param tenantName
	 * @return
	 */
	public Tenant createTenant(String tenantName);

	Tenant createTenant(int port, String tenantName);
	Tenant createTenant(int id, int port, String tenantName);

}
