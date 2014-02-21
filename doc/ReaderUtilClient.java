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

/*
 * This file is one of the source files for the examples contained in
 * /src/site/xdoc/examples.xml
 * It is not intended to be included in a source release.
 */

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * Instantiates and uses a ReaderUtil. The GenericObjectPool supplied to the constructor will have
 * default configuration properties.
 */
public class ReaderUtilClient {

    public static void main(String[] args) {
        ReaderUtil readerUtil = new ReaderUtil(new GenericObjectPool<StringBuffer>(new StringBufferFactory()));
        Reader reader = new StringReader("foo");
        try {
            System.out.println(readerUtil.readToString(reader));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
