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
package org.apache.commons.pool;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @version $Revision: 1.5 $ $Date: 2004/02/28 11:46:11 $
 * @author Rodney Waldhoff
 */
public class TestBaseObjectPool extends TestCase {
    public TestBaseObjectPool(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestBaseObjectPool.class);
    }
    
    // tests
    public void testUnsupportedOperations() throws Exception {
        ObjectPool pool = new BaseObjectPool() { 
            public Object borrowObject() throws Exception {
                return null;
            }
            public void returnObject(Object obj) throws Exception {                
            }
            public void invalidateObject(Object obj) throws Exception {                
            }            
        };   
        
        try {
            pool.getNumIdle();
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {
            // expected
        }

        try {
            pool.getNumActive();
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {
            // expected
        }

        try {
            pool.clear();
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {
            // expected
        }

        try {
            pool.addObject();
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {
            // expected
        }

        try {
            pool.setFactory(null);
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {
            // expected
        }
    }
}
