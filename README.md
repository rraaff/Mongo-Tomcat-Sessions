Mongo Tomcat Session Manager
============================

Overview
--------

This is a tomcat session manager that saves sessions in MongoDB.
It is made up of MongoManager, that provides the save/load functions, and MongoSessionTrackerValve that controls the
timing of the save.

For those interested, PersistentManager/ an implementation of a SessionStore is non deterministic in when it saves to the DB,
so isn't suitable as a distributed session replicator.  This is why I've used a valve to control the save timing at the end of each request.

Usage
-----

Add the following into your tomcat server.xml, or context.xml

    <Valve className="com.dawsonsystems.session.MemcachedSessionTrackerValve" />
    <Manager className="com.dawsonsystems.session.MemcachedManager" host="dbHost1" port="11211" maxInactiveInterval="1800" useProxySessions="true" suffix="prod"/>

The Valve must be before the Manager.

The following parameters are available on the Manager :-

<table>
<tr><td>maxInactiveInterval</td><td>The initial maximum time interval, in seconds, between client requests before a session is invalidated. A negative value will result in sessions never timing out. If the attribute is not provided, a default of 60 seconds is used.</td></tr>
<tr><td>processExpiresFrequency</td><td>Frequency of the session expiration, and related manager operations. Manager operations will be done once for the specified amount of backgrondProcess calls (i.e., the lower the amount, the more often the checks will occur). The minimum value is 1, and the default value is 6. </td></tr>
<tr><td>host</td><td>The database hostname</td></tr>
<tr><td>port</td><td>The database port to connect to. The default is 11211</td></tr>
</table>


Put the memcached-store jar and the memcached java driver  into the tomcat lib directory and you're good to go.