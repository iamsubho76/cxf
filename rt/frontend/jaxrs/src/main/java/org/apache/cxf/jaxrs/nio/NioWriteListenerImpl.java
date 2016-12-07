/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.nio;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import javax.servlet.WriteListener;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;

public final class NioWriteListenerImpl implements WriteListener {
    private static final Logger LOG = LogUtils.getL7dLogger(NioWriteListenerImpl.class);
    private Continuation cont;
    private final NioWriteEntity entity;
    private final DelegatingNioOutputStream out;

    public NioWriteListenerImpl(Continuation cont, NioWriteEntity entity, OutputStream out) {
        this.cont = cont;
        this.entity = entity;
        this.out = new DelegatingNioOutputStream(out);
    }

    @Override
    public void onWritePossible() throws IOException {
        while (cont.isReadyForWrite()) {
            if (!entity.getWriter().write(out)) {
                // REVISIT:
                // Immediately closing the async context with cont.resume() works better
                // at the moment - with cont.resume() Jetty throws NPE in its internal code
                // which is quite possibly a Jetty bug.
                // Do we really need to complete the out chain after the response has been written out ?
                cont.resume();
                return;
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        try {
            entity.getError().error(t);
        } catch (final Throwable ex) {
            LOG.warning("NIO WriteListener error: " + ExceptionUtils.getStackTrace(ex));
        } finally {
            cont.resume();
        }
    }
}