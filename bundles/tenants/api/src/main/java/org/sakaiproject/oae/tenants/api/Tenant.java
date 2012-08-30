package org.sakaiproject.oae.tenants.api;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Tenant {

	private int id;
	private String name;
	private int port;

	public Tenant(int id, int port, String name) {
		this.id = id;
		this.port = port;
		this.name = name;
	}

	@XmlElement
	public int getId() {
		return this.id;
	}

	@XmlElement
	public int getPort() {
		return this.port;
	}

	@XmlElement
	public String getName() {
		return this.name;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setName(String name) {
		this.name = name;
	}

}
