package io.jafka.ptth;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

/**
 * HTTP 客户端
 * 
 * @author adyliu (imxylz@gmail.com)
 * @since 2017年7月12日
 */
public class Http {

    private static final Log logger = LogFactory.getLog(Http.class);
    private static volatile CloseableHttpClient _httpClient;
    static final ThreadLocal<Integer> retryCount = new ThreadLocal<Integer>();
    static final ThreadLocal<Integer> executionCount = new ThreadLocal<>();
    private static final int CONNECTION_TIMEOUT = 30*1000;
    private static final int SO_TIMEOUT = 120*1000;

    private static synchronized void init() {
        if (_httpClient != null)
            return;
        // 连接池连接三十分钟有效
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(10, TimeUnit.MINUTES);
        connectionManager.setMaxTotal(200);
        connectionManager.setDefaultMaxPerRoute(100);
        SocketConfig socketConfig = SocketConfig.custom()//
                .setSoTimeout(SO_TIMEOUT).build();
        connectionManager.setDefaultSocketConfig(socketConfig);
        // HttpConnectionParams.setSoReuseaddr(params, true);
        _httpClient = HttpClients.custom()//
                .setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).build())//
                .setRetryHandler(new DefaultHttpRequestRetryHandler())//
                .setDefaultCookieStore(new EmptyCookieStore())//强制忽略cookie
                .setConnectionManager(connectionManager)//
                .build();
        //
    }

    static RequestConfig.Builder createRequestConfig() {
        return RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(SO_TIMEOUT);
    }

    /**
     * 关闭连接池
     */
    public static synchronized void shutdown() throws IOException {
        if (_httpClient != null) {
            _httpClient.close();
            _httpClient = null;
        }
    }

    /**
     * 创建或者打开一个HTTP连接池
     * 
     * @return HTTP 客户端
     */
    public static CloseableHttpClient getHttpClient() {
        if (_httpClient == null) {
            init();
        }
        return _httpClient;
    }

    /**
     * 执行一个HTTP 请求
     * 
     * @param req
     *            HTTP 请求
     * @return 响应结果
     * @throws IOException
     *             任何IO异常
     * @see {@link Request}
     * @see {@link Response}
     */
    public static Response execute(Request req) throws IOException {
        HttpRequestBase method;
        if ("GET".equalsIgnoreCase(req.method)) {
            method = new HttpGet(req.getUrl());
        } else if ("POST".equalsIgnoreCase(req.method)) {
            method = new HttpPost(req.getUrl());
        } else if ("PUT".equalsIgnoreCase(req.method)) {
            method = new HttpPut(req.getUrl());
        } else if ("DELETE".equalsIgnoreCase(req.method)) {
            method = new HttpDelete(req.getUrl());
        } else if("HEAD".equalsIgnoreCase(req.method)) {
            method = new HttpHead(req.getUrl());
        }
        else {
            throw new IllegalArgumentException("unsupported method " + req.method);
        }
        req.prepare(method);

        if (logger.isDebugEnabled()) {
            logger.debug(req.toDebugString());
        }
        try {
            retryCount.set(req.retry);
            executionCount.set(1);
            return execute(method, req.getHttpContext(), req.forceOk);
        } finally {
            retryCount.remove();
            executionCount.remove();
        }
    }

    public static Response execute(HttpRequestBase method, HttpContext httpContext, boolean forceOk) throws IOException {
        
        Response response = new Response();
        final long start = System.currentTimeMillis();
        try (CloseableHttpResponse resp = getHttpClient().execute(method, httpContext)) {
            //
            response.statusCode = resp.getStatusLine().getStatusCode();
            if (forceOk && !response.isScOk()) {
                throw new IOException("response not 200, statusCode=" + response.getStatusCode() + ", uri=" + method.getURI());
            }
            response.setHttpContext(httpContext);
            response.setHeaders(resp.getAllHeaders());
            HttpEntity entity = resp.getEntity();
            if (entity != null) {
                ContentType contentType = ContentType.get(entity);
                if (contentType != null) {
                    Charset charset = contentType.getCharset();
                    if (charset != null) {// set charset from response
                        response.setEncoding(charset);
                    }
                }
                response.copyStream(entity);
            }
            response.timecost = System.currentTimeMillis() - start;
            return response;
        } catch (IOException ioe) {
            method.abort();
            throw ioe;
        } finally {
            method.reset();
        }
    }
}