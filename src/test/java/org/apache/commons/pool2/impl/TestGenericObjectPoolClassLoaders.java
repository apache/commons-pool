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

import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;

import org.junit.Assert;
import org.junit.Test;

public class TestGenericObjectPoolClassLoaders {

    private static final URL BASE_URL =
            TestGenericObjectPoolClassLoaders.class.getResource(
                    "/org/apache/commons/pool2/impl/");

    @Test
    public void testContextClassLoader() throws Exception {

        ClassLoader savedClassloader =
                Thread.currentThread().getContextClassLoader();

        try {
            CustomClassLoader cl1 = new CustomClassLoader(1);
            Thread.currentThread().setContextClassLoader(cl1);
            CustomClassLoaderObjectFactory factory1 =
                    new CustomClassLoaderObjectFactory(1);
            GenericObjectPool<URL> pool1 = new GenericObjectPool<URL>(factory1);
            pool1.setMinIdle(1);
            pool1.setTimeBetweenEvictionRunsMillis(100);
            int counter = 0;
            while (counter < 50 && pool1.getNumIdle() != 1) {
                Thread.sleep(100);
                counter++;
            }
            Assert.assertEquals("Wrong number of idle objects in pool1", 1,
                    pool1.getNumIdle());

            // ---------------
            CustomClassLoader cl2 = new CustomClassLoader(2);
            Thread.currentThread().setContextClassLoader(cl2);
            CustomClassLoaderObjectFactory factory2 =
                    new CustomClassLoaderObjectFactory(2);
            GenericObjectPool<URL> pool2 =
                    new GenericObjectPool<URL>(factory2);
            pool2.setMinIdle(1);

            pool2.addObject();
            Assert.assertEquals("Wrong number of idle objects in pool2", 1,
                    pool2.getNumIdle());
            pool2.clear();

            pool2.setTimeBetweenEvictionRunsMillis(100);

            counter = 0;
            while (counter < 50 && pool2.getNumIdle() != 1) {
                Thread.sleep(100);
                counter++;
            }
            Assert.assertEquals("Wrong number of  idle objects in pool2", 1,
                    pool2.getNumIdle());

            pool1.close();
            pool2.close();
        } finally {
            Thread.currentThread().setContextClassLoader(savedClassloader);
        }
    }

    private static class CustomClassLoaderObjectFactory extends
            BasePooledObjectFactory<URL> {
        private int n;

        CustomClassLoaderObjectFactory(int n) {
            this.n = n;
        }

        @Override
        public URL create() throws Exception {
            URL url = Thread.currentThread().getContextClassLoader()
                    .getResource("test" + n);
            if (url == null) {
                throw new IllegalStateException("Object should not be null");
            }
            return url;
        }

        @Override
        public PooledObject<URL> wrap(URL value) {
            return new DefaultPooledObject<URL>(value);
        }
    }

    private static class CustomClassLoader extends URLClassLoader {
        private int n;

        CustomClassLoader(int n) {
            super(new URL[] { BASE_URL });
            this.n = n;
        }

        @Override
        public URL findResource(String name) {
            if (!name.endsWith(String.valueOf(n))) {
                return null;
            }

            return super.findResource(name);
        }
    }
}
