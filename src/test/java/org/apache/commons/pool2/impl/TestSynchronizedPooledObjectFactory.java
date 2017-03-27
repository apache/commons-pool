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
