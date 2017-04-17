package com.dawsonsystems.session;

import java.util.HashSet;
import java.util.Set;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class RequestContext {

	private String currentPath;
	private Request request;
	private Response response;
	private String initSessionId;
	
	private Set<String> invalidatedSessionCookies = new HashSet<String>();
	private String newSessionCookie;
	
	public RequestContext(Request request2, Response response2, String currentPath) {
		this.request = request2;
		this.response = response2;
		this.setCurrentPath(currentPath);
	}
	public Request getRequest() {
		return request;
	}
	public void setRequest(Request request) {
		this.request = request;
	}
	public Response getResponse() {
		return response;
	}
	public void setResponse(Response response) {
		this.response = response;
	}
	public String getInitSessionId() {
		return initSessionId;
	}
	public void setInitSessionId(String initSessionId) {
		this.initSessionId = initSessionId;
	}
	public Set<String> getInvalidatedSessionCookies() {
		return invalidatedSessionCookies;
	}
	public void setInvalidatedSessionCookies(Set<String> invalidatedSessionCookies) {
		this.invalidatedSessionCookies = invalidatedSessionCookies;
	}
	public String getNewSessionCookie() {
		return newSessionCookie;
	}
	public void setNewSessionCookie(String newSessionCookie) {
		this.newSessionCookie = newSessionCookie;
	}
	public void addInvalidatedSession(String id) {
		this.invalidatedSessionCookies.add(id);
		
	}
	public String getCurrentPath() {
		return currentPath;
	}
	public void setCurrentPath(String currentPath) {
		this.currentPath = currentPath;
	}
	
	
}
