/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.pool2.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * Base class that provides common functionality for {@link GenericObjectPool}
 * and {@link GenericKeyedObjectPool}. The primary reason this class exists is
 * reduce code duplication between the two pool implementations.
 */
public abstract class BaseGenericObjectPool implements NotificationEmitter {

    // Constants
    /**
     * Name of the JMX notification broadcast when the pool implementation
     * swallows an {@link Exception}. 
     */
    public static final String NOTIFICATION_SWALLOWED_EXCEPTION =
            "SWALLOWED_EXCEPTION";
    private static final int SWALLOWED_EXCEPTION_QUEUE_SIZE = 10;
    
    // Configuration attributes
    // None as yet
    
    // Internal state attributes
    volatile boolean closed = false;
    
    // Monitoring (primarily JMX) attributes
    private final NotificationBroadcasterSupport jmxNotificationSupport;
    private final String creationStackTrace;
    private final Deque<String> swallowedExceptions = new LinkedList<String>();
    private final AtomicInteger swallowedExcpetionCount = new AtomicInteger(0);

    
    public BaseGenericObjectPool(BaseObjectPoolConfig config) {
        if (config.getJmxEnabled()) {
            this.jmxNotificationSupport = new NotificationBroadcasterSupport();
        } else {
            this.jmxNotificationSupport = null;
        }
        
        // Populate the swallowed exceptions queue
        for (int i = 0; i < SWALLOWED_EXCEPTION_QUEUE_SIZE; i++) {
            swallowedExceptions.add(null);
        }

        // Populate the creation stack trace
        this.creationStackTrace = getStackTrace(new Exception());
    }

    /**
     * Closes the pool destroys the remaining idle objects and, if registered in
     * JMX, deregisters it.
     */
    public abstract void close();

    /**
     * Has this pool instance been closed.
     * @return <code>true</code> when this pool has been closed.
     */
    public final boolean isClosed() {
        return closed;
    }
    
    
    /**
     * Throws an <code>IllegalStateException</code> if called when the pool has
     * been closed.
     * 
     * @throws IllegalStateException if this pool has been closed.
     * @see #isClosed()
     */
    protected final void assertOpen() throws IllegalStateException {
        if (isClosed()) {
            throw new IllegalStateException("Pool not open");
        }
    }

    
    // Monitoring (primarily JMX) related methods
    
    /**
     * Provides the name under which the pool has been registered with the
     * platform MBean server or <code>null</code> if the pool has not been
     * registered.
     */
    public abstract ObjectName getJmxName();
    
    /**
     * Provides the stack trace for the call that created this pool. JMX
     * registration may trigger a memory leak so it is important that pools are
     * deregistered when no longer used by calling the {@link #close()} method.
     * This method is provided to assist with identifying code that creates but
     * does not close it thereby creating a memory leak.   
     */
    public String getCreationStackTrace() {
        return creationStackTrace;
    }

    /**
     * Lists the most recent exceptions that have been swallowed by the pool
     * implementation. Exceptions are typically swallowed when a problem occurs
     * while destroying an object.
     */
    public String[] getSwallowedExceptions() {
        List<String> temp =
                new ArrayList<String>(SWALLOWED_EXCEPTION_QUEUE_SIZE);
        synchronized (swallowedExceptions) {
            temp.addAll(swallowedExceptions);
        }
        return temp.toArray(new String[SWALLOWED_EXCEPTION_QUEUE_SIZE]);
    }

    protected final NotificationBroadcasterSupport getJmxNotificationSupport() {
        return jmxNotificationSupport;
    }
    
    protected String getStackTrace(Exception e) {
        // Need the exception in string form to prevent the retention of
        // references to classes in the stack trace that could trigger a memory
        // leak in a container environment
        Writer w = new StringWriter();
        PrintWriter pw = new PrintWriter(w);
        e.printStackTrace(pw);
        return w.toString();
    }
    
    protected void swallowException(Exception e) {
        String msg = getStackTrace(e);

        ObjectName oname = getJmxName();
        if (oname != null) {
            Notification n = new Notification(NOTIFICATION_SWALLOWED_EXCEPTION,
                    oname, swallowedExcpetionCount.incrementAndGet(), msg);
            getJmxNotificationSupport().sendNotification(n);
        }

        // Add the exception the queue, removing the oldest
        synchronized (swallowedExceptions) {
            swallowedExceptions.addLast(msg);
            swallowedExceptions.pollFirst();
        }
    }


    // Implement NotificationEmitter interface
        
    @Override
    public final void addNotificationListener(NotificationListener listener,
            NotificationFilter filter, Object handback)
            throws IllegalArgumentException {

        if (jmxNotificationSupport == null) {
            throw new UnsupportedOperationException("JMX is not enabled");
        }
        jmxNotificationSupport.addNotificationListener(
                listener, filter, handback);
    }

    @Override
    public final void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {

        if (jmxNotificationSupport == null) {
            throw new UnsupportedOperationException("JMX is not enabled");
        }
        jmxNotificationSupport.removeNotificationListener(listener);
    }

    @Override
    public final MBeanNotificationInfo[] getNotificationInfo() {

        if (jmxNotificationSupport == null) {
            throw new UnsupportedOperationException("JMX is not enabled");
        }
        return jmxNotificationSupport.getNotificationInfo();
    }

    @Override
    public final void removeNotificationListener(NotificationListener listener,
            NotificationFilter filter, Object handback)
            throws ListenerNotFoundException {

        if (jmxNotificationSupport == null) {
            throw new UnsupportedOperationException("JMX is not enabled");
        }
        jmxNotificationSupport.removeNotificationListener(
                listener, filter, handback);
    }
}
