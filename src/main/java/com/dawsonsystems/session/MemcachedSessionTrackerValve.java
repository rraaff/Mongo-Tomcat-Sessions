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
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

public class MemcachedSessionTrackerValve extends ValveBase {
	private static Logger log = Logger.getLogger("MemcachedSessionValve");
	private MemcachedManager manager;

	public void setMongoManager(MemcachedManager manager) {
		this.manager = manager;
	}

	@Override
	public void invoke(Request request, Response response) throws IOException,
			ServletException {
		String path = getFirstPathSegment(request);
		manager.setCurrentRequest(request, response, path);
		try {
			getNext().invoke(request, response);
		} finally {
			try {
				storeSession(request, response);
			} catch (ExecutionException e) {
				log.fine("error saving to memcache" + e.getMessage());
			}
			manager.clearCurrentRequest();
		}
	}

	private String getFirstPathSegment(Request request) {
		String url = request.getRequestURI();
		if (url.startsWith("/")) {
			url = url.substring(1);
		}
		int end = url.indexOf('/');
		if (end > 0) {
			url = url.substring(0, end);
		}
		return url;
	}

	private void storeSession(Request request, Response response)
			throws IOException, ExecutionException {
		Session sessionFromReq = request.getSessionInternal(false);
		Session session = manager.getCurrentSessionThreadLocal();
		String initSessionId = MemcachedManager.getInitSessionId();
		
		if (sessionFromReq != null && session != null && initSessionId != null && !initSessionId.equals(session.getId())) {
			Cookie c = getSessionCookie(request);
			if (c != null) {
				c.setValue(session.getId());
				//response.addCookie(c);
				//response.addSessionCookieInternal(c);
				//response.setHeader("JSESSIONID", session.getId());
			}
		}
		
		for (String sessionId : MemcachedManager.getCurrentRequest().getInvalidatedSessionCookies()) {
			manager.remove(sessionId);
		}

		if (session != null) {
			
			if (session.isValid()) {
				if (log.isLoggable(Level.FINE)) {
					log.fine("Request with session completed, saving session "
							+ session.getId());
				}
				if (session.getSession() != null) {
					if (session instanceof MemcachedProxySession) {
						if (((MemcachedProxySession) session).isProxy()) {
							if (log.isLoggable(Level.FINE)) {
								log.fine("HTTP Session is proxy, Not saving "
									+ session.getId());
							}
						} else {
							if (log.isLoggable(Level.FINE)) {
								log.fine("HTTP Session present, saving "
									+ session.getId());
							}
							manager.save(session);
						}
					} else {
						if (log.isLoggable(Level.FINE)) {
							log.fine("HTTP Session present, saving "
								+ session.getId());
						}
						manager.save(session);
					}
				} else {
					if (log.isLoggable(Level.FINE)) {
						log.fine("No HTTP Session present, Not saving "
							+ session.getId());
					}
				}
			} else {
				if (log.isLoggable(Level.FINE)) {
					log.fine("HTTP Session has been invalidated, removing :"
						+ session.getId());
				}
				manager.remove(session);
				
				request.getSession(true).invalidate();
				request.setRequestedSessionId(null);
				request.clearCookies();

				  // step 3: create a new session and set it to the request
				  Session newSession = request.getSessionInternal(true);
				  request.setRequestedSessionId(newSession.getId());
				  manager.add(session);
				  manager.save(session);
				
			}
		}
	}

	private Cookie getSessionCookie(Request req) {
		for (Cookie c : req.getCookies()) {
			if (c.getName().equals("JSESSIONID")) {
				return c;
			}
		}
		return null;
	}
}
