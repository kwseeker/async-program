package top.kwseeker.reactor.netty.webflux.http.server.reactive;

import java.net.InetSocketAddress;

import org.springframework.http.HttpRequest;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

public interface ServerHttpRequest extends HttpRequest, ReactiveHttpInputMessage {

	//String getId();

	RequestPath getPath();

	MultiValueMap<String, String> getQueryParams();

	//MultiValueMap<String, HttpCookie> getCookies();

	@Nullable
	default InetSocketAddress getRemoteAddress() {
		return null;
	}

	//default ServerHttpRequest.Builder mutate() {
	//	return new DefaultServerHttpRequestBuilder(this);
	//}


	/**
	 * Builder for mutating an existing {@link ServerHttpRequest}.
	 */
	//interface Builder {
	//
	//	/**
	//	 * Set the HTTP method to return.
	//	 */
	//	Builder method(HttpMethod httpMethod);
	//
	//	/**
	//	 * Set the URI to use with the following conditions:
	//	 * <ul>
	//	 * <li>If {@link #path(String) path} is also set, it overrides the path
	//	 * of the URI provided here.
	//	 * <li>If {@link #contextPath(String) contextPath} is also set, or
	//	 * already present, it must match the start of the path of the URI
	//	 * provided here.
	//	 * </ul>
	//	 */
	//	Builder uri(URI uri);
	//
	//	/**
	//	 * Set the path to use instead of the {@code "rawPath"} of the URI of
	//	 * the request with the following conditions:
	//	 * <ul>
	//	 * <li>If {@link #uri(URI) uri} is also set, the path given here
	//	 * overrides the path of the given URI.
	//	 * <li>If {@link #contextPath(String) contextPath} is also set, or
	//	 * already present, it must match the start of the path given here.
	//	 * <li>The given value must begin with a slash.
	//	 * </ul>
	//	 */
	//	Builder path(String path);
	//
	//	/**
	//	 * Set the contextPath to use.
	//	 * <p>The given value must be a valid {@link RequestPath#contextPath()
	//	 * contextPath} and it must match the start of the path of the URI of
	//	 * the request. That means changing the contextPath, implies also
	//	 * changing the path via {@link #path(String)}.
	//	 */
	//	Builder contextPath(String contextPath);
	//
	//	/**
	//	 * Set or override the specified header values under the given name.
	//	 * <p>If you need to add header values, remove headers, etc., use
	//	 * {@link #headers(Consumer)} for greater control.
	//	 * @param headerName the header name
	//	 * @param headerValues the header values
	//	 * @since 5.1.9
	//	 * @see #headers(Consumer)
	//	 */
	//	Builder header(String headerName, String... headerValues);
	//
	//	/**
	//	 * Manipulate request headers. The provided {@code HttpHeaders} contains
	//	 * current request headers, so that the {@code Consumer} can
	//	 * {@linkplain HttpHeaders#set(String, String) overwrite} or
	//	 * {@linkplain HttpHeaders#remove(Object) remove} existing values, or
	//	 * use any other {@link HttpHeaders} methods.
	//	 * @see #header(String, String...)
	//	 */
	//	Builder headers(Consumer<HttpHeaders> headersConsumer);
	//
	//	/**
	//	 * Set the SSL session information. This may be useful in environments
	//	 * where TLS termination is done at the router, but SSL information is
	//	 * made available in some other way such as through a header.
	//	 * @since 5.0.7
	//	 */
	//	Builder sslInfo(SslInfo sslInfo);
	//
	//	/**
	//	 * Build a {@link ServerHttpRequest} decorator with the mutated properties.
	//	 */
	//	ServerHttpRequest build();
	//}

}