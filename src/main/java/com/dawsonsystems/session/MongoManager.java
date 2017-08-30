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

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.Valve;
import org.apache.catalina.session.StandardSession;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteResult;

public class MongoManager implements Manager, Lifecycle {
	private static Logger log = Logger.getLogger("MongoManager");
	protected static String host = "localhost";
	protected static String suffix = "";
	protected static int port = 27017;
	protected static String database = "sessions";
	
	/* Absolute Path containing properties for mongo username and password*/
	private static String credentialFilePath = "";
	/* username key for the credentiales file poperties */
	private static String userNameKey = "";
	/* password key for the credentiales file poperties */
	private static String passwordKey = "";
	
	protected MongoClient mongo;
	protected DB db;
	protected boolean slaveOk;
	
	private LifecycleState state = LifecycleState.NEW;

	protected static String useProxySessions = "false";

	private MongoSessionTrackerValve trackerValve;
	
	private ThreadLocal<String> currentPath = new ThreadLocal<>();
	
	private ThreadLocal<String> currentSessionPath = new ThreadLocal<>();
	private ThreadLocal<StandardSession> currentSession = new ThreadLocal<>();
	private Serializer serializer;

	// Either 'kryo' or 'java'
	private String serializationStrategyClass = "com.dawsonsystems.session.JavaSerializer";

	private Container container;
	private int maxInactiveInterval;

	protected void setCurrentPath(String path) {
		currentPath.set(path);
	}
	
	protected String getCurrentPath() {
		String result = currentPath.get();
		if (result == null || result.length() == 0){
			result = "";
		}
		return result;
	}
	
	public void clearCurrentPath() {
		currentPath.remove();
	}
	
	protected void setCurrentSessionPath(String path) {
		currentSessionPath.set(path);
	}
	
	protected String getCurrentSessionPath() {
		String result = currentSessionPath.get();
		if (result == null || result.length() == 0){
			result = "";
		}
		return result;
	}
	
	public void clearCurrentSessionPath() {
		currentSessionPath.remove();
	}

	@Override
	public Container getContainer() {
		return container;
	}

	@Override
	public void setContainer(Container container) {
		this.container = container;
	}

	@Override
	public boolean getDistributable() {
		return false;
	}

	@Override
	public void setDistributable(boolean b) {
		// nothing to do
	}

	@Override
	public String getInfo() {
		return "Mongo Session Manager";
	}

	@Override
	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	@Override
	public void setMaxInactiveInterval(int i) {
		maxInactiveInterval = i;
	}

	@Override
	public int getSessionIdLength() {
		return 37;
	}

	@Override
	public void setSessionIdLength(int i) {
		// nothing to do
	}


	@Override
	public int getMaxActive() {
		return 1000000;
	}

	@Override
	public void setMaxActive(int i) {
		// nothing to do
	}

	@Override
	public int getActiveSessions() {
		return 1000000;
	}


	@Override
	public int getRejectedSessions() {
		return 0;
	}

	public void setSerializationStrategyClass(String strategy) {
		this.serializationStrategyClass = strategy;
	}

	public void setSlaveOk(boolean slaveOk) {
		this.slaveOk = slaveOk;
	}

	public boolean getSlaveOk() {
		return slaveOk;
	}

	public void setRejectedSessions(int i) {
		// nothing to do
	}

	@Override
	public int getSessionMaxAliveTime() {
		return maxInactiveInterval;
	}

	@Override
	public void setSessionMaxAliveTime(int i) {
		// nothing to do
	}

	@Override
	public int getSessionAverageAliveTime() {
		return 0;
	}

	@Override
	public void load() throws ClassNotFoundException, IOException {
		// nothing to do
	}

	@Override
	public void unload() throws IOException {
		// nothing to do
	}

	@Override
	public void backgroundProcess() {
		processExpires();
	}

	@Override
	public void addLifecycleListener(LifecycleListener lifecycleListener) {
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {
		return new LifecycleListener[0]; // To change body of implemented
											// methods use File | Settings |
											// File Templates.
	}

	@Override
	public void removeLifecycleListener(LifecycleListener lifecycleListener) {
		// nothing to do
	}

	@Override
	public void add(Session session) {
		try {
			save(session);
		} catch (IOException ex) {
			log.log(Level.SEVERE, "Error adding new session", ex);
		}
	}

	@Override
	public void addPropertyChangeListener(
			PropertyChangeListener propertyChangeListener) {
		// To change body of implemented methods use File | Settings | File
		// Templates.
	}

	@Override
	public void changeSessionId(Session session) {
		session.setId(UUID.randomUUID().toString());
	}

	@Override
	public Session createEmptySession() {
		StandardSession session = isUsingProxySessions() ? new MongoProxySession(this) : new MongoSession(this);
		session.setId(UUID.randomUUID().toString());
		session.setMaxInactiveInterval(maxInactiveInterval);
		session.setValid(true);
		session.setCreationTime(System.currentTimeMillis());
		session.setNew(true);
		setCurrentSessionThreadLocal(session);
		if (log.isLoggable(Level.FINE)) {
			log.fine("Created new empty session " + session.getIdInternal());
		}
		return session;
	}

	private void setCurrentSessionThreadLocal(StandardSession session) {
		currentSession.set(session);
		currentSessionPath.set(getCurrentPath());
	}

	/**
	 * @deprecated
	 */
	public org.apache.catalina.Session createSession() {
		return createEmptySession();
	}

	@Override
	public org.apache.catalina.Session createSession(java.lang.String sessionId) {
		StandardSession session = (StandardSession) createEmptySession();
		if (log.isLoggable(Level.FINE)) {
			log.fine("Created session with id " + session.getIdInternal()
					+ " ( " + sessionId + ")");
		}
		if (sessionId != null) {
			session.setId(sessionId);
		}

		return session;
	}

	@Override
	public org.apache.catalina.Session[] findSessions() {
		try {
			List<Session> sessions = new ArrayList<Session>();
			for (String sessionId : keys()) {
				sessions.add(loadSession(sessionId));
			}
			return sessions.toArray(new Session[sessions.size()]);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	protected org.apache.catalina.session.StandardSession getNewSession() {
		if (log.isLoggable(Level.FINE)) {
			log.fine("getNewSession()");
		}
		return (StandardSession) createEmptySession();
	}

	@Override
	public void start() throws LifecycleException {
		for (Valve valve : getContainer().getPipeline().getValves()) {
			if (valve instanceof MongoSessionTrackerValve) {
				trackerValve = (MongoSessionTrackerValve) valve;
				trackerValve.setMongoManager(this);
				if (log.isLoggable(Level.INFO)) {
					log.info("Attached to Mongo Tracker Valve");
				}
				break;
			}
		}
		try {
			initSerializer();
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
			log.log(Level.SEVERE, "Unable to load serializer", e);
			throw new LifecycleException(e);
		}
		if (log.isLoggable(Level.INFO)) {
			log.info("Will expire sessions after " + getMaxInactiveInterval()
					+ " seconds");
		}
		initDbConnection();
	}

	@Override
	public void stop() throws LifecycleException {
		state = LifecycleState.STOPPING;
		mongo.close();
		state = LifecycleState.STOPPED;
	}

	@Override
	public Session findSession(String id) throws IOException {
		return loadSession(id);
	}

	public static String getHost() {
		return host;
	}

	public static void setHost(String host) {
		MongoManager.host = host;
	}

	public static int getPort() {
		return port;
	}

	public static void setPort(int port) {
		MongoManager.port = port;
	}

	public static String getDatabase() {
		return database;
	}

	public static void setDatabase(String database) {
		MongoManager.database = database;
	}

	public void clear() throws IOException {
		//getCollection().drop();
	}

	private DBCollection getCollection() throws IOException {
		return db.getCollection("sessions");
	}

	public int getSize() throws IOException {
		return (int) getCollection().count();
	}

	public String[] keys() throws IOException {

		BasicDBObject restrict = new BasicDBObject();
		restrict.put("_id", 1);

		DBCursor cursor = getCollection().find(new BasicDBObject(), restrict);

		List<String> ret = new ArrayList<>();

		while (cursor.hasNext()) {
			ret.add(cursor.next().get("").toString());
		}

		return ret.toArray(new String[ret.size()]);
	}
	
	private boolean isUsingProxySessions() {
		return "true".equals(useProxySessions);
	}

	public Session loadSession(String id) throws IOException {

		if (id == null || id.length() == 0) {
			return createEmptySession();
		}

		StandardSession session = currentSession.get();

		if (session != null) {
			if (id.equals(session.getId()) && getCurrentPath().equals(currentSessionPath.get())) {
				return session;
			} else {
				currentSession.remove();
			}
		}
		if (isUsingProxySessions()) {
			session = (StandardSession) createEmptySession();
			session.setId(id);
			setCurrentSessionThreadLocal(session);
			return session;
		} else {
			try {
				return loadFromDb(id, (StandardSession)createEmptySession());
		    } catch (ClassNotFoundException ex) {
		      log.log(Level.SEVERE, "Unable to deserialize session ", ex);
		      throw new IOException("Unable to deserializeInto session", ex);
		    }
		}
	}

	public StandardSession loadFromDb(String id, StandardSession session)
			throws IOException, ClassNotFoundException {
		if (log.isLoggable(Level.FINE)) {
			log.fine("Loading session " + id + " from Mongo");
		}
		BasicDBObject query = new BasicDBObject();
		query.put("_id", getMongoSessionKey(id));

		DBObject dbsession = getCollection().findOne(query);

		if (dbsession == null) {
			if (log.isLoggable(Level.FINE)) {
				log.fine("Session " + id + " not found in Mongo");
			}
			StandardSession ret = getNewSession();
			ret.setId(id);
			setCurrentSessionThreadLocal(ret);
			return ret;
		}

		byte[] data = (byte[]) dbsession.get("data");

		session.setId(id);
		session.setManager(this);
		serializer.deserializeInto(data, session);

		session.setMaxInactiveInterval(-1);
		session.access();
		session.setValid(true);
		session.setNew(false);

		if (log.isLoggable(Level.FINE)) {
			log.fine("Session Contents [" + session.getId() + "]:");
			for (Object name : Collections.list(session.getAttributeNames())) {
				log.fine("  " + name);
			}
		}
		if (log.isLoggable(Level.FINE)) {
			log.fine("Loaded session id " + id);
		}
		if (!isUsingProxySessions()) {
			setCurrentSessionThreadLocal(session);
		}
	    return session;
	}

	private Object getMongoSessionKey(String id) {
		StringBuilder result = new StringBuilder(id);
		String currentPath = getCurrentPath();
		String suffix = getSuffix();
		if (currentPath != null && !currentPath.equals("")) {
			result.append("-").append(currentPath);
		}
		if (suffix != null && !suffix.equals("")) {
			result.append("-").append(suffix);
		}
		return result.toString();
	}

	public void save(Session session) throws IOException {
		try {
			if (log.isLoggable(Level.FINE)) {
				log.fine("Saving session " + session + " into Mongo");
			}

			StandardSession standardsession = (StandardSession) session;

			if (log.isLoggable(Level.FINE)) {
				log.fine("Session Contents [" + session.getId() + "]:");
				for (Object name : Collections.list(standardsession
						.getAttributeNames())) {
					log.fine("  " + name);
				}
			}

			byte[] data = serializer.serializeFrom(standardsession);

			BasicDBObject dbsession = new BasicDBObject();
			dbsession.put("_id", getMongoSessionKey(standardsession.getId()));
			dbsession.put("data", data);
			dbsession.put("lastmodified", System.currentTimeMillis());

			BasicDBObject query = new BasicDBObject();
			query.put("_id", getMongoSessionKey(standardsession.getIdInternal()));
			getCollection().update(query, dbsession, true, false);
			log.fine("Updated session with id " + session.getIdInternal());
		} catch (IOException e) {
			log.severe(e.getMessage());
			throw e;
		} finally {
			currentSession.remove();
			if (log.isLoggable(Level.FINE)) {
				log.fine("Session removed from ThreadLocal :"
					+ session.getIdInternal());
			}
		}
	}

	@Override
	public void remove(Session session) {
		if (log.isLoggable(Level.FINE)) {
			log.fine("Removing session ID : " + session.getId());
		}
		BasicDBObject query = new BasicDBObject();
		query.put("_id", getMongoSessionKey(session.getId()));

		try {
			getCollection().remove(query);
		} catch (IOException e) {
			log.log(Level.SEVERE,
					"Error removing session in Mongo Session Store", e);
		} finally {
			currentSession.remove();
		}
	}
	
	
	
	@Override
	public void remove(Session arg0, boolean arg1) {
		remove(arg0);
	}

	@Override
	public void removePropertyChangeListener(
			PropertyChangeListener propertyChangeListener) {
		// To change body of implemented methods use File | Settings | File
		// Templates.
	}

	public void processExpires() {
		BasicDBObject query = new BasicDBObject();

		long olderThan = System.currentTimeMillis()
				- (getMaxInactiveInterval() * 1000);
		if (log.isLoggable(Level.FINE)) {
			log.fine("Looking for sessions less than for expiry in Mongo : "
				+ olderThan);
		}

		query.put("lastmodified", new BasicDBObject("$lt", olderThan));

		try {
			WriteResult result = getCollection().remove(query);
			if (log.isLoggable(Level.FINE)) {
				log.fine("Expired sessions : " + result.getN());
			}
		} catch (IOException e) {
			log.log(Level.SEVERE,
					"Error cleaning session in Mongo Session Store", e);
		}
	}

	private void initDbConnection() throws LifecycleException {
		try {
			String[] hosts = getHost().split(",");

			List<ServerAddress> addrs = new ArrayList<>();

			for (String host : hosts) {
				addrs.add(new ServerAddress(host, getPort()));
			}
			
			List<MongoCredential> credentials = new ArrayList<>();
	
			if (hasCredentialFilePath()) {
				SensitiveConf sensitiveConf = SensitiveConf.load(getCredentialFilePath());
				credentials.add(MongoCredential.createCredential(sensitiveConf.getProperty(getUserNameKey()), getDatabase(), sensitiveConf.getProperty(getPasswordKey()).toCharArray()));
			} 
			mongo = new MongoClient(addrs,credentials);
			db = mongo.getDB(getDatabase());
			if (slaveOk) {
				db.slaveOk();
			}
			if (log.isLoggable(Level.FINE)) {
				log.info("Connected to Mongo " + host + "/" + database
					+ " for session storage, slaveOk=" + slaveOk + ", "
					+ (getMaxInactiveInterval() * 1000) + " session live time");
			}
		} catch (IOException e) {
			throw new LifecycleException("Error Connecting to Mongo", e);
		}
	}

	private static boolean hasCredentialFilePath() {
		if (getCredentialFilePath() == null) {
			return false;
		}
		return getCredentialFilePath().trim().length() > 0;
	}

	private void initSerializer() throws ClassNotFoundException,
			IllegalAccessException, InstantiationException {
		if (log.isLoggable(Level.FINE)) {
			log.info("Attempting to use serializer :" + serializationStrategyClass);
		}
		serializer = (Serializer) Class.forName(serializationStrategyClass)
				.newInstance();

		Loader loader = null;

		if (container != null) {
			loader = container.getLoader();
		}
		ClassLoader classLoader = null;

		if (loader != null) {
			classLoader = loader.getClassLoader();
		}
		serializer.setClassLoader(classLoader);
	}
	
	public String getUseProxySessions() {
		return useProxySessions;
	}

	public void setUseProxySessions(String useProxySessions) {
		MongoManager.useProxySessions = useProxySessions;
	}

	@Override
	public void init() throws LifecycleException {
		// nothing to do
	}

	@Override
	public void destroy() throws LifecycleException {
		// nothing to do
	}

	@Override
	public LifecycleState getState() {
		return state;
	}

	@Override
	public String getStateName() {
		return state.toString();
	}

	@Override
	public long getSessionCounter() {
		return 10000000;
	}

	@Override
	public void setSessionCounter(long sessionCounter) {
		// nothing to do
	}

	@Override
	public long getExpiredSessions() {
		return 0;
	}

	@Override
	public void setExpiredSessions(long expiredSessions) {
		// nothing to do
	}

	@Override
	public int getSessionCreateRate() {
		return 0;
	}

	@Override
	public int getSessionExpireRate() {
		return 0;
	}

	public static String getSuffix() {
		return suffix;
	}

	public static void setSuffix(String suffix) {
		MongoManager.suffix = suffix;
	}

	public boolean willAttributeDistribute(String arg0, Object arg1) {
		return true;
	}
	
	public static String getPasswordKey() {
		return passwordKey;
	}

	public static void setPasswordKey(String passwordKey) {
		MongoManager.passwordKey = passwordKey;
	}

	public static String getUserNameKey() {
		return userNameKey;
	}

	public static void setUserNameKey(String userNameKey) {
		MongoManager.userNameKey = userNameKey;
	}

	public static String getCredentialFilePath() {
		return credentialFilePath;
	}

	public static void setCredentialFilePath(String credentialFilePath) {
		MongoManager.credentialFilePath = credentialFilePath;
	}
	
}
