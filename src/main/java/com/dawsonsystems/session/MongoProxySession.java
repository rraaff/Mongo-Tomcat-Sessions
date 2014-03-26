/***********************************************************************************************************************
 *
 * Mongo Tomcat Sessions
 * ==========================================
 *
 * Copyright (C) 2012 by Dawson Systems Ltd (http://www.dawsonsystems.com)
 *
 ***********************************************************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/

package com.dawsonsystems.session;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.SessionListener;
import org.apache.catalina.session.StandardSession;

public class MongoProxySession extends StandardSession {

	/**
	 * 
	 */
	private static final long serialVersionUID = -674522109957698646L;
	private static Logger log = Logger.getLogger("MongoManager");

	private boolean isValid = true;

	private boolean isProxy = true;

	public boolean isProxy() {
		return isProxy;
	}

	public void setProxy(boolean isProxy) {
		this.isProxy = isProxy;
	}

	public MongoProxySession(Manager manager) {
		super(manager);
	}

	@Override
	protected boolean isValidInternal() {
		return isValid;
	}

	@Override
	public boolean isValid() {
		return isValidInternal();
	}

	private void unproxy() {
		try {
			if (!this.isProxy) {
				return;
			}
			((MongoManager) getManager()).loadFromDb(this.id, this);
			this.isProxy = false;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Unable to unproxy", e);
		} catch (ClassNotFoundException e) {
			log.log(Level.SEVERE, "Unable to unproxy", e);
		}
	}

	@Override
	public void setValid(boolean isValid) {
		this.isValid = isValid;
		if (!isValid) {
			String keys[] = keys();
			for (String key : keys) {
				removeAttributeInternal(key, false);
			}
			getManager().remove(this);

		}
	}

	@Override
	public void invalidate() {
		unproxy();
		setValid(false);
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public void access() {
		if (!this.isProxy) {
			super.access();
		}
	}

	@Override
	public void activate() {
		unproxy();
		super.activate();
	}

	@Override
	public void addSessionListener(SessionListener listener) {
		unproxy();
		super.addSessionListener(listener);
	}

	@Override
	public void endAccess() {
		super.endAccess();
	}

	@Override
	protected boolean exclude(String arg0) {
		unproxy();
		return super.exclude(arg0);
	}

	@Override
	public void expire() {
		unproxy();
		super.expire();
	}

	@Override
	public void expire(boolean arg0) {
		unproxy();
		super.expire(arg0);
	}

	@Override
	protected void fireContainerEvent(Context context, String type, Object data)
			throws Exception {
		unproxy();
		super.fireContainerEvent(context, type, data);
	}

	@Override
	public void fireSessionEvent(String arg0, Object arg1) {
		unproxy();
		super.fireSessionEvent(arg0, arg1);
	}

	@Override
	public Object getAttribute(String name) {
		unproxy();
		return super.getAttribute(name);
	}

	@Override
	public Enumeration getAttributeNames() {
		unproxy();
		return super.getAttributeNames();
	}

	@Override
	public String getAuthType() {
		unproxy();
		return super.getAuthType();
	}

	@Override
	public long getCreationTime() {
		unproxy();
		return super.getCreationTime();
	}

	@Override
	public String getId() {
		return super.getId();
	}

	@Override
	public String getIdInternal() {
		return this.id;
	}

	@Override
	public String getInfo() {
		unproxy();
		return super.getInfo();
	}

	@Override
	public long getLastAccessedTime() {
		unproxy();
		return super.getLastAccessedTime();
	}

	@Override
	public long getLastAccessedTimeInternal() {
		unproxy();
		return super.getLastAccessedTimeInternal();
	}

	@Override
	public Manager getManager() {
		return super.getManager();
	}

	@Override
	public int getMaxInactiveInterval() {
		unproxy();
		return super.getMaxInactiveInterval();
	}

	@Override
	public Object getNote(String name) {
		unproxy();
		return super.getNote(name);
	}

	@Override
	public Iterator getNoteNames() {
		unproxy();
		return super.getNoteNames();
	}

	@Override
	public Principal getPrincipal() {
		unproxy();
		return super.getPrincipal();
	}

	@Override
	public ServletContext getServletContext() {
		unproxy();
		return super.getServletContext();
	}

	@Override
	public HttpSession getSession() {
		return this;
	}

	@Override
	public HttpSessionContext getSessionContext() {
		unproxy();
		return super.getSessionContext();
	}

	@Override
	public Object getValue(String name) {
		unproxy();
		return super.getValue(name);
	}

	@Override
	public String[] getValueNames() {
		unproxy();
		return super.getValueNames();
	}

	@Override
	public boolean isNew() {
		unproxy();
		return super.isNew();
	}

	@Override
	protected String[] keys() {
		unproxy();
		return super.keys();
	}

	@Override
	public void passivate() {
		unproxy();
		super.passivate();
	}

	@Override
	public void putValue(String name, Object value) {
		unproxy();
		super.putValue(name, value);
	}

	@Override
	protected void readObject(ObjectInputStream arg0)
			throws ClassNotFoundException, IOException {
		unproxy();
		super.readObject(arg0);
	}

	@Override
	public void readObjectData(ObjectInputStream stream)
			throws ClassNotFoundException, IOException {
		unproxy();
		super.readObjectData(stream);
	}

	@Override
	public void recycle() {
		unproxy();
		super.recycle();
	}

	@Override
	public void removeAttribute(String name, boolean notify) {
		unproxy();
		super.removeAttribute(name, notify);
	}

	@Override
	public void removeAttribute(String name) {
		unproxy();
		super.removeAttribute(name);
	}

	@Override
	protected void removeAttributeInternal(String arg0, boolean arg1) {
		unproxy();
		super.removeAttributeInternal(arg0, arg1);
	}

	@Override
	public void removeNote(String name) {
		unproxy();
		super.removeNote(name);
	}

	@Override
	public void removeSessionListener(SessionListener listener) {
		unproxy();
		super.removeSessionListener(listener);
	}

	@Override
	public void removeValue(String name) {
		unproxy();
		super.removeValue(name);
	}

	@Override
	public void setAttribute(String arg0, Object arg1, boolean arg2) {
		unproxy();
		super.setAttribute(arg0, arg1, arg2);
	}

	@Override
	public void setAttribute(String name, Object value) {
		unproxy();
		super.setAttribute(name, value);
	}

	@Override
	public void setAuthType(String authType) {
		unproxy();
		super.setAuthType(authType);
	}

	@Override
	public void setCreationTime(long time) {
		super.setCreationTime(time);
	}

	@Override
	public void setManager(Manager manager) {
		super.setManager(manager);
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		super.setMaxInactiveInterval(interval);
	}

	@Override
	public void setNew(boolean isNew) {
		super.setNew(isNew);
	}

	@Override
	public void setNote(String name, Object value) {
		unproxy();
		super.setNote(name, value);
	}

	@Override
	public void setPrincipal(Principal principal) {
		unproxy();
		super.setPrincipal(principal);
	}

	@Override
	public void tellNew() {
		unproxy();
		super.tellNew();
	}

	@Override
	public String toString() {
		return super.toString();
	}

	@Override
	protected void writeObject(ObjectOutputStream arg0) throws IOException {
		unproxy();
		super.writeObject(arg0);
	}

	@Override
	public void writeObjectData(ObjectOutputStream stream) throws IOException {
		unproxy();
		super.writeObjectData(stream);
	}

}
