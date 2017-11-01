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

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;

/**
 * Copies PoolUtil's private static class SynchronizedPooledObjectFactory.
 *
 * A fully synchronized PooledObjectFactory that wraps a PooledObjectFactory and
 * synchronizes access to the wrapped factory methods.
 * <p>
 * <b>Note:</b> This should not be used on pool implementations that already
 * provide proper synchronization such as the pools provided in the Commons Pool
 * library.
 * </p>
 */
final class TestSynchronizedPooledObjectFactory<T> implements PooledObjectFactory<T> {

	/** Synchronization lock */
	private final WriteLock writeLock = new ReentrantReadWriteLock().writeLock();

	/** Wrapped factory */
	private final PooledObjectFactory<T> factory;

	/**
	 * Create a SynchronizedPoolableObjectFactory wrapping the given factory.
	 *
	 * @param factory
	 *            underlying factory to wrap
	 * @throws IllegalArgumentException
	 *             if the factory is null
	 */
	TestSynchronizedPooledObjectFactory(final PooledObjectFactory<T> factory) throws IllegalArgumentException {
		if (factory == null) {
			throw new IllegalArgumentException("factory must not be null.");
		}
		this.factory = factory;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PooledObject<T> makeObject() throws Exception {
		writeLock.lock();
		try {
			return factory.makeObject();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void destroyObject(final PooledObject<T> p) throws Exception {
		writeLock.lock();
		try {
			factory.destroyObject(p);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validateObject(final PooledObject<T> p) {
		writeLock.lock();
		try {
			return factory.validateObject(p);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void activateObject(final PooledObject<T> p) throws Exception {
		writeLock.lock();
		try {
			factory.activateObject(p);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void passivateObject(final PooledObject<T> p) throws Exception {
		writeLock.lock();
		try {
			factory.passivateObject(p);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("SynchronizedPoolableObjectFactory");
		sb.append("{factory=").append(factory);
		sb.append('}');
		return sb.toString();
	}
}
