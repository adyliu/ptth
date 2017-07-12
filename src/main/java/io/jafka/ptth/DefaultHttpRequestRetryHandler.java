/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package io.jafka.ptth;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

/**
 * The default {@link DefaultHttpRequestRetryHandler} used by request executors.
 * 
 * 
 * @since 4.0
 */
class DefaultHttpRequestRetryHandler implements HttpRequestRetryHandler {

    public DefaultHttpRequestRetryHandler() {
    }

    /**
     * Used <code>retryCount</code> and <code>requestSentRetryEnabled</code> to determine if the given method should be retried.
     */
    public boolean retryRequest(final IOException exception, int executionCount, final HttpContext context) {
        int retryCount = Http.retryCount.get() != null ? Http.retryCount.get().intValue() : 0;
        final boolean retry = retryCount > 0;
        if (exception == null) {
            throw new IllegalArgumentException("Exception parameter may not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("HTTP context may not be null");
        }
        Http.executionCount.set(executionCount);//记录执行了几次
        if (executionCount > retryCount) {
            // Do not retry if over max retry count
            return false;
        }
        if (exception instanceof InterruptedIOException && !retry) {
            // Timeout
            return false;
        }
        if (exception instanceof UnknownHostException) {
            // Unknown host
            return false;
        }
        if (exception instanceof ConnectException && !retry) {
            // Connection refused
            // retry even connection refused
            return false;
        }
        if (exception instanceof SSLException) {
            // SSL handshake exception
            return false;
        }

        HttpRequest request = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);

        if (requestIsAborted(request)) {
            return false;
        }

        if (handleAsIdempotent(request)) {
            // Retry if the request is considered idempotent
            return true;
        }

        Boolean b = (Boolean) context.getAttribute(ExecutionContext.HTTP_REQ_SENT);
        boolean sent = (b != null && b.booleanValue());

        if (!sent || retryCount > 0) {
            // Retry if the request has not been sent fully or
            // if it's OK to retry methods that have been sent
            return true;
        }
        // otherwise do not retry
        return false;
    }

    /**
     * @since 4.2
     */
    protected boolean handleAsIdempotent(final HttpRequest request) {
        return !(request instanceof HttpEntityEnclosingRequest);
    }

    /**
     * @since 4.2
     */
    protected boolean requestIsAborted(final HttpRequest request) {
        HttpRequest req = request;
        if (request instanceof RequestWrapper) { // does not forward request to original
            req = ((RequestWrapper) request).getOriginal();
        }
        return (req instanceof HttpUriRequest && ((HttpUriRequest) req).isAborted());
    }

}
