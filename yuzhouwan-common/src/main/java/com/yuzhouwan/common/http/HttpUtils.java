package com.yuzhouwan.common.http;

import com.alibaba.fastjson.JSON;
import com.yuzhouwan.common.util.PropUtils;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CodingErrorAction;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Copyright @ 2016 yuzhouwan.com
 * All right reserved.
 * Function: HttpUtils
 *
 * @author Benedict Jin
 * @since 2016/3/21
 */
public class HttpUtils {

    private static final Logger _log = LoggerFactory.getLogger(HttpUtils.class);

    private static volatile HttpUtils helper;

    private static String userAgent;

    private static int TIMEOUT_CONNECTION;
    private static int TIMEOUT_SOCKET;
    private static int MAX_TOTAL;
    private static int MAX_RETRY;
    private static int MAX_ROUTE_TOTAL;

    static {
        TIMEOUT_CONNECTION = Integer.valueOf(PropUtils.getInstance().getProperty("TIMEOUT_CONNECTION"));
        TIMEOUT_SOCKET = Integer.valueOf(PropUtils.getInstance().getProperty("TIMEOUT_SOCKET"));
        MAX_TOTAL = Integer.valueOf(PropUtils.getInstance().getProperty("MAX_TOTAL"));
        MAX_RETRY = Integer.valueOf(PropUtils.getInstance().getProperty("MAX_RETRY"));
        MAX_ROUTE_TOTAL = Integer.valueOf(PropUtils.getInstance().getProperty("MAX_ROUTE_TOTAL"));
    }

    private CloseableHttpClient httpClient;
    private HttpClientContext httpClientContext;
    private TrustManager[] trustManagers = new TrustManager[1];

    private HttpUtils() {
    }

    public static HttpUtils getInstance() {
        if (helper == null)
            synchronized (HttpUtils.class) {
                if (helper == null) {
                    helper = new HttpUtils();
                    helper.initialize();
                }
            }
        return helper;
    }

    public static void destory() {
        try {
            if (helper != null && helper.httpClient != null)
                helper.httpClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getUserAgent() {
        return userAgent;
    }

    public static void setUserAgent(String userAgent) {
        HttpUtils.userAgent = userAgent;
    }

    private void initialize() {
        // Connection配置
        ConnectionConfig connectionConfig = ConnectionConfig.custom().setMalformedInputAction(CodingErrorAction.IGNORE)
                .setUnmappableInputAction(CodingErrorAction.IGNORE).setCharset(Consts.UTF_8).build();

        // 覆盖证书检测过程 [用以非CA的https链接 (CA, Certificate Authority 数字证书)]
        trustManagers[0] = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        // httpClient上下文
        httpClientContext = HttpClientContext.create();
        httpClientContext.setCookieStore(new BasicCookieStore());

        SSLConnectionSocketFactory sslSocketFactory;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[0], trustManagers, new SecureRandom());
            sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        } catch (Exception e) {
            _log.error("error: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        // 创建 httpClient连接池
        PoolingHttpClientConnectionManager httpClientConnectionManager = new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create().
                        register("http", PlainConnectionSocketFactory.getSocketFactory()).
                        register("https", sslSocketFactory).build());
        httpClientConnectionManager.setMaxTotal(MAX_TOTAL); // 设置连接池线程最大数量
        httpClientConnectionManager.setDefaultMaxPerRoute(MAX_ROUTE_TOTAL); // 设置单个路由最大的连接线程数量
        httpClientConnectionManager.setDefaultConnectionConfig(connectionConfig);

        // 默认请求配置
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(TIMEOUT_SOCKET)
                .setConnectTimeout(TIMEOUT_CONNECTION)
                .setConnectionRequestTimeout(TIMEOUT_CONNECTION).build();

        // 设置重定向策略
        LaxRedirectStrategy redirectStrategy = new LaxRedirectStrategy();

        // 重试策略
        HttpRequestRetryHandler retryHandler = (IOException exception, int executionCount, HttpContext context) -> {
            if (executionCount >= MAX_RETRY) {
                // Do not retry if over max retry count
                return false;
            }
            if (exception instanceof NoHttpResponseException) {
                // Retry if the server dropped connection on us
                return true;
            }
            if (exception instanceof SSLHandshakeException) {
                // Do not retry on SSL handshake exception
                return false;
            }
            HttpRequest request = (HttpRequest) context.getAttribute(HttpCoreContext.HTTP_REQUEST);
            // Retry if the request is considered idempotent
            return !(request instanceof HttpEntityEnclosingRequest);
        };

        // 初始化 httpClient客户端
        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setConnectionManager(httpClientConnectionManager)
                .setDefaultRequestConfig(defaultRequestConfig)
                .setRedirectStrategy(redirectStrategy)
                .setRetryHandler(retryHandler);

        if (HttpUtils.userAgent != null) {
            httpClientBuilder.setUserAgent(userAgent);
        }
        httpClient = httpClientBuilder.build();
    }

    private MultipartEntityBuilder processBuilderParams(Map<String, Object> params) {

        ContentType contentType = ContentType.TEXT_PLAIN.withCharset(Consts.UTF_8);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        if (params != null) {
            _log.debug(params.toString());
            Object value;
            for (Entry<String, Object> entry : params.entrySet()) {
                value = entry.getValue();
                if (value instanceof File) {
                    builder.addBinaryBody(entry.getKey(), (File) value);
                } else if (value instanceof CharSequence) {
                    builder.addTextBody(entry.getKey(), value.toString(), contentType);
                } else {
                    builder.addTextBody(entry.getKey(), JSON.toJSONString(value), contentType);
                }
            }
        }
        return builder;
    }

    /**
     * POST with params
     *
     * @param url
     * @param params
     * @param headers
     * @return
     * @throws Exception
     */
    public InputStream postStream(String url, Map<String, Object> params, Map<String, String> headers) throws Exception {
        return checkResponseStatus(post(url, params, headers)).getStream();
    }

    public String postPlain(String url, Map<String, Object> params, Map<String, String> headers) throws Exception {
        return checkResponseStatus(post(url, params, headers)).getText();
    }

    public HttpResponse post(String url, Map<String, Object> params, Map<String, String> headers) throws Exception {
        HttpPost post = new HttpPost(url);
        _log.debug(url);
        MultipartEntityBuilder builder = processBuilderParams(params);
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.setCharset(Consts.UTF_8);
        HttpEntity entity = builder.build();
        post.setEntity(entity);
        processHeader(post, headers);
        return internalProcess(post);
    }

    /**
     * POST with HttpEntity
     *
     * @param url
     * @param entityParam
     * @param headers
     * @return
     * @throws Exception
     */
    public InputStream postStream(String url, HttpEntity entityParam, Map<String, String> headers) throws Exception {
        return checkResponseStatus(post(url, entityParam, headers)).getStream();
    }

    public String postPlain(String url, HttpEntity entityParam, Map<String, String> headers) throws Exception {
        return checkResponseStatus(post(url, entityParam, headers)).getText();
    }

    public HttpResponse post(String url, HttpEntity entity, Map<String, String> headers) throws Exception {
        HttpPost post = new HttpPost(url);
        _log.debug(url);
        post.setEntity(entity);
        processHeader(post, headers);
        return internalProcess(post);
    }

    /**
     * GET with params, except HttpEntity
     *
     * @param url
     * @param params
     * @param headers
     * @return
     * @throws Exception
     */
    public InputStream getStream(String url, Map<String, Object> params, Map<String, String> headers) throws Exception {
        return checkResponseStatus(get(url, params, headers)).getStream();
    }

    public String getPlain(String url, Map<String, Object> params, Map<String, String> headers) throws Exception {
        return checkResponseStatus(get(url, params, headers)).getText();
    }

    public HttpResponse get(String url, Map<String, Object> params, Map<String, String> headers) throws Exception {
        HttpGet get = new HttpGet(processURL(url, params));
        _log.debug(url);
        processHeader(get, headers);
        return internalProcess(get);
    }

    /**
     * PUT with params
     *
     * @param url
     * @param params
     * @param headers
     * @return
     * @throws Exception
     */
    public InputStream putStream(String url, Map<String, Object> params, Map<String, String> headers) throws Exception {
        return checkResponseStatus(put(url, params, headers)).getStream();
    }

    public String putPlain(String url, Map<String, Object> params, Map<String, String> headers) throws Exception {
        return checkResponseStatus(put(url, params, headers)).getText();
    }

    public HttpResponse put(String url, Map<String, Object> params, Map<String, String> headers) throws Exception {
        HttpPut put = new HttpPut(processURL(url, params));
        _log.debug(url);
        processHeader(put, headers);
        return internalProcess(put);
    }

    /**
     * PUT with HttpEntity
     *
     * @param url
     * @param entityParam
     * @param headers
     * @return
     * @throws Exception
     */
    public InputStream putStream(String url, HttpEntity entityParam, Map<String, String> headers) throws Exception {
        return checkResponseStatus(put(url, entityParam, headers)).getStream();
    }

    public String putPlain(String url, HttpEntity entityParam, Map<String, String> headers) throws Exception {
        return checkResponseStatus(put(url, entityParam, headers)).getText();
    }

    public HttpResponse put(String url, HttpEntity entity, Map<String, String> headers) throws Exception {
        HttpPut put = new HttpPut(url);
        _log.debug(url);
        put.setEntity(entity);
        processHeader(put, headers);
        return internalProcess(put);
    }

    /**
     * DELETE with params, except HttpEntity
     *
     * @param url
     * @param params
     * @param headers
     * @return
     * @throws Exception
     */
    public InputStream deleteStream(String url, Map<String, Object> params, Map<String, String> headers) throws Exception {
        return checkResponseStatus(delete(url, params, headers)).getStream();
    }

    public String deletePlain(String url, Map<String, Object> params, Map<String, String> headers) throws Exception {
        return checkResponseStatus(delete(url, params, headers)).getText();
    }

    public HttpResponse delete(String url, Map<String, Object> params, Map<String, String> headers) throws Exception {
        HttpDelete delete = new HttpDelete(processURL(url, params));
        _log.debug(url);
        processHeader(delete, headers);
        return internalProcess(delete);
    }

    /**
     * RESTful 内部操作
     *
     * @param rest
     * @return
     * @throws IOException
     */
    private HttpResponse internalProcess(HttpRequestBase rest) throws IOException {
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(rest, httpClientContext);
            int statusCode = response.getStatusLine().getStatusCode();
            HttpResponse httpResponse = new HttpResponse();
            httpResponse.setCode(statusCode);
            if (httpResponse.isError()) {
                _log.error("error response: status {}, method {} ", statusCode, rest.getMethod());
            }
            httpResponse.setBytes(EntityUtils.toByteArray(response.getEntity()));
            Header header;
            if ((header = response.getEntity().getContentType()) != null)
                httpResponse.setContentType(header.getValue());
            return httpResponse;
        } catch (Exception e) {
            _log.error("error: {}", e.getMessage());
            throw e;
        } finally {
            release(rest, response, response != null ? response.getEntity() : null);
        }
    }

    /**
     * 检查 HttpResponse's Code [装饰器模式]
     *
     * @param response
     */
    private HttpResponse checkResponseStatus(HttpResponse response) {
        if (response.isError())
            throw new RuntimeException(String.format("Response is error, code: %s", response.getCode()));
        return response;
    }

    private void processHeader(HttpRequestBase entity, Map<String, String> headers) {
        if (headers == null) {
            return;
        }
        for (Entry<String, String> entry : headers.entrySet()) {
            entity.addHeader(entry.getKey(), entry.getValue());
        }
    }

    private String processURL(String processUrl, Map<String, Object> params) {
        if (params == null) {
            return processUrl;
        }
        _log.debug(params.toString());
        StringBuilder url = new StringBuilder(processUrl);
        if (url.indexOf("?") < 0)
            url.append('?');

        for (String name : params.keySet()) {
            url.append('&');
            url.append(name);
            url.append('=');
            url.append(String.valueOf(params.get(name)));
        }
        return url.toString().replace("?&", "?");
    }

    private void release(HttpRequestBase request, CloseableHttpResponse response, HttpEntity entity) {
        try {
            if (request != null)
                request.releaseConnection();
        } finally {
            try {
                if (entity != null)
                    EntityUtils.consume(entity);
            } catch (IOException e) {
                _log.error("error: {}", e.getMessage());
            } finally {
                try {
                    if (response != null)
                        response.close();
                } catch (IOException e) {
                    _log.error("error: {}", e.getMessage());
                }
            }
        }
    }
}
