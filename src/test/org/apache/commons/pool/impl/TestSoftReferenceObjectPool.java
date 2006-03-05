/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.pool.impl;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.TestObjectPool;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Rodney Waldhoff
 * @version $Revision$ $Date$
 */
public class TestSoftReferenceObjectPool extends TestObjectPool {
    public TestSoftReferenceObjectPool(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestSoftReferenceObjectPool.class);
    }

    protected ObjectPool makeEmptyPool(int cap) {
        return new SoftReferenceObjectPool(
            new PoolableObjectFactory()  {
                int counter = 0;
                public Object makeObject() { return String.valueOf(counter++); }
                public void destroyObject(Object obj) { }
                public boolean validateObject(Object obj) { return true; }
                public void activateObject(Object obj) { }
                public void passivateObject(Object obj) { }
            }
            );
    }

    protected Object getNthObject(int n) {
        return String.valueOf(n);
    }

    private List testFactorySequenceStates = new ArrayList(5);
    public void testFactorySequence() throws Exception {
        // setup
        // We need a factory that tracks method call sequence.
        PoolableObjectFactory pof = new PoolableObjectFactory() {
            public Object makeObject() throws Exception {
                testFactorySequenceStates.add("makeObject");
                return new Object();
            }

            public void activateObject(Object obj) throws Exception {
                testFactorySequenceStates.add("activateObject");
            }

            public boolean validateObject(Object obj) {
                testFactorySequenceStates.add("validateObject");
                return true;
            }

            public void passivateObject(Object obj) throws Exception {
                testFactorySequenceStates.add("passivateObject");
            }

            public void destroyObject(Object obj) throws Exception {
                testFactorySequenceStates.add("destroyObject");
            }
        };

        ObjectPool pool = new SoftReferenceObjectPool(pof);

        // check the order in which the factory is called during borrow
        testFactorySequenceStates.clear();
        Object o = pool.borrowObject();
        List desiredSequence = Arrays.asList(new String[] {
                "makeObject",
                "activateObject",
                "validateObject"
        });
        assertEquals("Wrong sequence", desiredSequence, testFactorySequenceStates);

        // check the order in which the factory is called when returning an object
        testFactorySequenceStates.clear();
        pool.returnObject(o);
        desiredSequence = Arrays.asList(new String[] {
                "validateObject",
                "passivateObject"
        });
        assertEquals("Wrong sequence", desiredSequence, testFactorySequenceStates);

        // check the order in which the factory is called during borrow again
        testFactorySequenceStates.clear();
        o = pool.borrowObject();
        desiredSequence = Arrays.asList(new String[] {
                "activateObject",
                "validateObject"
        });
        assertEquals("Wrong sequence", desiredSequence, testFactorySequenceStates);

        // check the order in which the factory is called when invalidating an object
        testFactorySequenceStates.clear();
        pool.invalidateObject(o);
        desiredSequence = Arrays.asList(new String[] {
                "destroyObject"
        });
        assertEquals("Wrong sequence", desiredSequence, testFactorySequenceStates);
    }

    protected boolean isLifo() {
        return false;
    }

    protected boolean isFifo() {
        return false;
    }

}
