package com.zz.common.http.builder;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import com.zz.common.http.exception.HttpProcessException;
import com.zz.common.http.model.SSLs;
import com.zz.common.http.model.SSLs.SSLProtocolVersion;
import com.zz.common.util.PropertiesUtil;

public class HttpClientBuilderEx extends HttpClientBuilder {

	private boolean isPoolSet = false;

	private SSLProtocolVersion sslpv = SSLProtocolVersion.SSLv3;// ssl 协议版本

	private static int default_timeout = 1500;
	private static int default_keep_alive = 60;

	static {
		default_timeout = Integer.parseInt(PropertiesUtil.getValue("con_timeout"));
		default_keep_alive = Integer.parseInt(PropertiesUtil.getValue("keep_alive"));
	}

	// 用于配置ssl
	private SSLs ssls = SSLs.getInstance();

	private HttpClientBuilderEx() {
	}

	public static HttpClientBuilderEx custom() {
		return new HttpClientBuilderEx();
	}

	public HttpClientBuilderEx timeout() {
		return timeout(default_timeout, true);
	}

	public HttpClientBuilderEx timeout(int timeout) {
		return timeout(timeout, true);
	}

	/**
	 * 设置超时时间以及是否允许网页重定向（自动跳转 302）
	 * 
	 * @param timeout
	 *            超时时间，单位-毫秒
	 * @param redirectEnable
	 *            自动跳转
	 * @return 返回当前对象
	 */
	public HttpClientBuilderEx timeout(int timeout, boolean redirectEnable) {
		// 配置请求的超时设置
		RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(timeout).setConnectTimeout(timeout)
				.setSocketTimeout(timeout).setRedirectsEnabled(redirectEnable).build();
		return (HttpClientBuilderEx) this.setDefaultRequestConfig(config);
	}

	/**
	 * 设置连接池（默认开启https）
	 * 
	 * @param maxTotal
	 *            最大连接数
	 * @param defaultMaxPerRoute
	 *            每个路由默认连接数
	 * @return 返回当前对象
	 * @throws HttpProcessException
	 *             http处理异常
	 */
	public HttpClientBuilderEx pool(int maxTotal, int defaultMaxPerRout) throws HttpProcessException {
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.INSTANCE).register("https", ssls.getSSLCONNSF(sslpv))
				.build();
		// 设置连接池大小
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		connManager.setMaxTotal(maxTotal);// Increase max total connection to $maxTotal
		connManager.setDefaultMaxPerRoute(defaultMaxPerRout);// Increase default max connection per route to
																// $defaultMaxPerRoute
		// $route(eg：localhost:80) to 50
		isPoolSet = true;
		return (HttpClientBuilderEx) this.setConnectionManager(connManager);
	}

	/**
	 * 启用默认默认keepAlive时间
	 * 
	 * @return HttpClientBuilderEx
	 */
	public HttpClientBuilderEx keepAlive() {
		return (HttpClientBuilderEx) this.keepAlive(default_keep_alive);
	}

	/**
	 * 设置keepAlive时间
	 * 
	 * @param keepTime
	 * @return HttpClientBuilderEx
	 */
	public HttpClientBuilderEx keepAlive(int keepTime) {
		ConnectionKeepAliveStrategy keepAliveStrategy = new ConnectionKeepAliveStrategy() {
			@Override
			public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
				HeaderElementIterator it = new BasicHeaderElementIterator(
						response.headerIterator(HTTP.CONN_KEEP_ALIVE));
				while (it.hasNext()) {
					HeaderElement he = it.nextElement();
					String param = he.getName();
					String value = he.getValue();
					if (value != null && param.equalsIgnoreCase("timeout")) {
						return Long.parseLong(value) * 1000;
					}
				}
				return keepTime;// 如果没有约定，则默认定义时长为60s
			}
		};
		return (HttpClientBuilderEx) this.setKeepAliveStrategy(keepAliveStrategy);
	}

	/**
	 * 设置ssl安全链接
	 * 
	 * @return 返回当前对象
	 * @throws HttpProcessException
	 *             http处理异常
	 */
	public HttpClientBuilderEx ssl() throws HttpProcessException {
		return (HttpClientBuilderEx) this.setSSLSocketFactory(ssls.getSSLCONNSF(sslpv));
	}

	/**
	 * 设置自定义sslcontext
	 * 
	 * @param keyStorePath
	 *            密钥库路径
	 * @return 返回当前对象
	 * @throws HttpProcessException
	 *             http处理异常
	 */
	public HttpClientBuilderEx ssl(String keyStorePath) throws HttpProcessException {
		return ssl(keyStorePath, "nopassword");
	}

	/**
	 * 设置自定义sslcontext
	 * 
	 * @param keyStorePath
	 *            密钥库路径
	 * @param keyStorepass
	 *            密钥库密码
	 * @return 返回当前对象
	 * @throws HttpProcessException
	 *             http处理异常
	 */
	public HttpClientBuilderEx ssl(String keyStorePath, String keyStorepass) throws HttpProcessException {
		this.ssls = SSLs.custom().customSSL(keyStorePath, keyStorepass);
		return ssl();
	}

	/**
	 * 
	 * @return 是否设置了连接池
	 */
	public boolean isPoolSet() {
		return this.isPoolSet;
	}

}
