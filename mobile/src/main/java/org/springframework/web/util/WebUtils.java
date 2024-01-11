/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * Miscellaneous utilities for web applications.
 * <p>Used by various framework classes.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
public abstract class WebUtils {

	/**
	 * Standard Servlet 2.3+ spec request attribute for include request URI.
	 * <p>If included via a {@code RequestDispatcher}, the current resource will see the
	 * originating request. Its own request URI is exposed as a request attribute.
	 */
	public static final String INCLUDE_REQUEST_URI_ATTRIBUTE = "jakarta.servlet.include.request_uri";

	/**
	 * Standard Servlet 2.3+ spec request attribute for include context path.
	 * <p>If included via a {@code RequestDispatcher}, the current resource will see the
	 * originating context path. Its own context path is exposed as a request attribute.
	 */
	public static final String INCLUDE_CONTEXT_PATH_ATTRIBUTE = "jakarta.servlet.include.context_path";

	/**
	 * Standard Servlet 2.3+ spec request attribute for include servlet path.
	 * <p>If included via a {@code RequestDispatcher}, the current resource will see the
	 * originating servlet path. Its own servlet path is exposed as a request attribute.
	 */
	public static final String INCLUDE_SERVLET_PATH_ATTRIBUTE = "jakarta.servlet.include.servlet_path";

	/**
	 * Standard Servlet 2.3+ spec request attribute for include path info.
	 * <p>If included via a {@code RequestDispatcher}, the current resource will see the
	 * originating path info. Its own path info is exposed as a request attribute.
	 */
	public static final String INCLUDE_PATH_INFO_ATTRIBUTE = "jakarta.servlet.include.path_info";

	/**
	 * Standard Servlet 2.3+ spec request attribute for include query string.
	 * <p>If included via a {@code RequestDispatcher}, the current resource will see the
	 * originating query string. Its own query string is exposed as a request attribute.
	 */
	public static final String INCLUDE_QUERY_STRING_ATTRIBUTE = "jakarta.servlet.include.query_string";

	/**
	 * Standard Servlet 2.4+ spec request attribute for forward request URI.
	 * <p>If forwarded to via a RequestDispatcher, the current resource will see its
	 * own request URI. The originating request URI is exposed as a request attribute.
	 */
	public static final String FORWARD_REQUEST_URI_ATTRIBUTE = "jakarta.servlet.forward.request_uri";

	/**
	 * Standard Servlet 2.4+ spec request attribute for forward context path.
	 * <p>If forwarded to via a RequestDispatcher, the current resource will see its
	 * own context path. The originating context path is exposed as a request attribute.
	 */
	public static final String FORWARD_CONTEXT_PATH_ATTRIBUTE = "jakarta.servlet.forward.context_path";

	/**
	 * Standard Servlet 2.4+ spec request attribute for forward servlet path.
	 * <p>If forwarded to via a RequestDispatcher, the current resource will see its
	 * own servlet path. The originating servlet path is exposed as a request attribute.
	 */
	public static final String FORWARD_SERVLET_PATH_ATTRIBUTE = "jakarta.servlet.forward.servlet_path";

	/**
	 * Standard Servlet 2.4+ spec request attribute for forward path info.
	 * <p>If forwarded to via a RequestDispatcher, the current resource will see its
	 * own path ingo. The originating path info is exposed as a request attribute.
	 */
	public static final String FORWARD_PATH_INFO_ATTRIBUTE = "jakarta.servlet.forward.path_info";

	/**
	 * Standard Servlet 2.4+ spec request attribute for forward query string.
	 * <p>If forwarded to via a RequestDispatcher, the current resource will see its
	 * own query string. The originating query string is exposed as a request attribute.
	 */
	public static final String FORWARD_QUERY_STRING_ATTRIBUTE = "jakarta.servlet.forward.query_string";

	/**
	 * Standard Servlet 2.3+ spec request attribute for error page status code.
	 * <p>To be exposed to JSPs that are marked as error pages, when forwarding
	 * to them directly rather than through the servlet container's error page
	 * resolution mechanism.
	 */
	public static final String ERROR_STATUS_CODE_ATTRIBUTE = "jakarta.servlet.error.status_code";

	/**
	 * Standard Servlet 2.3+ spec request attribute for error page exception type.
	 * <p>To be exposed to JSPs that are marked as error pages, when forwarding
	 * to them directly rather than through the servlet container's error page
	 * resolution mechanism.
	 */
	public static final String ERROR_EXCEPTION_TYPE_ATTRIBUTE = "jakarta.servlet.error.exception_type";

	/**
	 * Standard Servlet 2.3+ spec request attribute for error page message.
	 * <p>To be exposed to JSPs that are marked as error pages, when forwarding
	 * to them directly rather than through the servlet container's error page
	 * resolution mechanism.
	 */
	public static final String ERROR_MESSAGE_ATTRIBUTE = "jakarta.servlet.error.message";

	/**
	 * Standard Servlet 2.3+ spec request attribute for error page exception.
	 * <p>To be exposed to JSPs that are marked as error pages, when forwarding
	 * to them directly rather than through the servlet container's error page
	 * resolution mechanism.
	 */
	public static final String ERROR_EXCEPTION_ATTRIBUTE = "jakarta.servlet.error.exception";

	/**
	 * Standard Servlet 2.3+ spec request attribute for error page request URI.
	 * <p>To be exposed to JSPs that are marked as error pages, when forwarding
	 * to them directly rather than through the servlet container's error page
	 * resolution mechanism.
	 */
	public static final String ERROR_REQUEST_URI_ATTRIBUTE = "jakarta.servlet.error.request_uri";

	/**
	 * Standard Servlet 2.3+ spec request attribute for error page servlet name.
	 * <p>To be exposed to JSPs that are marked as error pages, when forwarding
	 * to them directly rather than through the servlet container's error page
	 * resolution mechanism.
	 */
	public static final String ERROR_SERVLET_NAME_ATTRIBUTE = "jakarta.servlet.error.servlet_name";

	/**
	 * Prefix of the charset clause in a content type String: ";charset=".
	 */
	public static final String CONTENT_TYPE_CHARSET_PREFIX = ";charset=";

	/**
	 * Default character encoding to use when {@code request.getCharacterEncoding}
	 * returns {@code null}, according to the Servlet spec.
	 * @see ServletRequest#getCharacterEncoding
	 */
	public static final String DEFAULT_CHARACTER_ENCODING = "ISO-8859-1";

	/**
	 * Standard Servlet spec context attribute that specifies a temporary
	 * directory for the current web application, of type {@code java.io.File}.
	 */
	public static final String TEMP_DIR_CONTEXT_ATTRIBUTE = "jakarta.servlet.context.tempdir";

	/**
	 * HTML escape parameter at the servlet context level
	 * (i.e. a context-param in {@code web.xml}): "defaultHtmlEscape".
	 */
	public static final String HTML_ESCAPE_CONTEXT_PARAM = "defaultHtmlEscape";

	/**
	 * Use of response encoding for HTML escaping parameter at the servlet context level
	 * (i.e. a context-param in {@code web.xml}): "responseEncodedHtmlEscape".
	 * @since 4.1.2
	 */
	public static final String RESPONSE_ENCODED_HTML_ESCAPE_CONTEXT_PARAM = "responseEncodedHtmlEscape";

	/**
	 * Web app root key parameter at the servlet context level
	 * (i.e. a context-param in {@code web.xml}): "webAppRootKey".
	 */
	public static final String WEB_APP_ROOT_KEY_PARAM = "webAppRootKey";

	/** Default web app root key: "webapp.root". */
	public static final String DEFAULT_WEB_APP_ROOT_KEY = "webapp.root";

	/** Name suffixes in case of image buttons. */
	public static final String[] SUBMIT_IMAGE_SUFFIXES = {".x", ".y"};

	/** Key for the mutex session attribute. */
	public static final String SESSION_MUTEX_ATTRIBUTE = WebUtils.class.getName() + ".MUTEX";
}
