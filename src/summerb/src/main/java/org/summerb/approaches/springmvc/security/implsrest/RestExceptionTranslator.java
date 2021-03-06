package org.summerb.approaches.springmvc.security.implsrest;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;
import org.summerb.approaches.jdbccrud.common.DtoBase;
import org.summerb.approaches.security.api.CurrentUserNotFoundException;
import org.summerb.approaches.security.api.dto.NotAuthorizedResult;
import org.summerb.approaches.security.api.exceptions.NotAuthorizedException;
import org.summerb.approaches.springmvc.security.SecurityMessageCodes;
import org.summerb.approaches.springmvc.security.apis.JsonResponseWriter;
import org.summerb.approaches.validation.FieldValidationException;
import org.summerb.utils.exceptions.ExceptionUtils;
import org.summerb.utils.exceptions.dto.ExceptionInfo;
import org.summerb.utils.exceptions.dto.GenericServerErrorResult;
import org.summerb.utils.exceptions.translator.ExceptionTranslator;

/**
 * Request filter that helps to format and gracefully communicate server errors
 * to client.
 * 
 * Normally it's added to Spring security filter chain, i.e. like this:
 * 
 * <pre>
 * 	&lt;bean id=&quot;filterChainProxy&quot; class=&quot;org.springframework.security.web.FilterChainProxy&quot;&gt;
 * 		&lt;sec:filter-chain-map request-matcher=&quot;ant&quot;&gt;
 * 			&lt;sec:filter-chain pattern=&quot;/static/**&quot; filters=&quot;none&quot; /&gt;
 * 			&lt;sec:filter-chain pattern=&quot;/login/invalid-session&quot; filters=&quot;none&quot; /&gt;
 * 			&lt;sec:filter-chain pattern=&quot;/rest/**&quot;
 * 				filters=&quot;ipToMdcContext, securityContextFilter, restLogoutFilter, restLoginFilter, rememberMeFilter, servletApiFilter, anonFilter,
 * 				restSessionMgmtFilter, restExceptionTranslator, filterSecurityInterceptor&quot; /&gt;
 * 			&lt;sec:filter-chain pattern=&quot;/**&quot;
 * 				filters=&quot;ipToMdcContext, securityContextFilter, logoutFilter, formLoginFilter, rememberMeFilter, requestCacheFilter,
 * 	             servletApiFilter, anonFilter, sessionMgmtFilter, exceptionTranslator, filterSecurityInterceptor&quot; /&gt;
 * 		&lt;/sec:filter-chain-map&gt;
 * 	&lt;/bean&gt;
 * </pre>
 * 
 * @author sergeyk
 *
 */
public class RestExceptionTranslator extends GenericFilterBean {
	private Logger log = Logger.getLogger(getClass());
	public static final String X_TRANSLATE_AUTHORIZATION_ERRORS = "X-TranslateAuthorizationErrors";

	private AuthenticationTrustResolver authenticationTrustResolver;
	private JsonResponseWriter jsonResponseHelper;

	private ExceptionTranslator exceptionTranslator;

	public RestExceptionTranslator() {
		jsonResponseHelper = new JsonResponseWriterGsonImpl();
		authenticationTrustResolver = new AuthenticationTrustResolverImpl();
	}

	public RestExceptionTranslator(JsonResponseWriter jsonResponseHelper,
			AuthenticationTrustResolver authenticationTrustResolver) {
		this.jsonResponseHelper = jsonResponseHelper;
		this.authenticationTrustResolver = authenticationTrustResolver;
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		try {
			chain.doFilter(request, response);
			log.trace("Chain processed normally");
		} catch (Exception ex) {
			log.info("Unhandled exception while processing REST query", ex);

			DtoBase result = determineFailureResult(ex, request, response);
			jsonResponseHelper.writeResponseBody(result, response);
		}
	}

	private DtoBase determineFailureResult(Exception ex, HttpServletRequest request, HttpServletResponse response) {
		// first see if it is FVE
		FieldValidationException fve = ExceptionUtils.findExceptionOfType(ex, FieldValidationException.class);
		if (fve != null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return fve.getErrorDescriptionObject();
		}

		boolean translateAuthErrors = Boolean.TRUE
				.equals(Boolean.valueOf(request.getHeader(X_TRANSLATE_AUTHORIZATION_ERRORS)));
		GenericServerErrorResult ret = null;
		if (translateAuthErrors) {
			ret = new GenericServerErrorResult(
					exceptionTranslator.buildUserMessage(ex, LocaleContextHolder.getLocale()), new ExceptionInfo(ex));
		}

		NotAuthorizedException naex = ExceptionUtils.findExceptionOfType(ex, NotAuthorizedException.class);
		if (naex != null) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return ret != null ? ret : naex.getResult();
		}

		AuthenticationException ae = ExceptionUtils.findExceptionOfType(ex, AuthenticationException.class);
		if (ae != null) {
			// NOTE: See how we did that in AuthenticationFailureHandlerImpl...
			// Looks like we need to augment our custom RestLoginFilter so it
			// will put username to request
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return ret != null ? ret
					: new NotAuthorizedResult("(username not resolved)", SecurityMessageCodes.AUTH_FATAL);
		}

		AccessDeniedException ade = ExceptionUtils.findExceptionOfType(ex, AccessDeniedException.class);
		if (ade != null) {
			if (authenticationTrustResolver.isAnonymous(SecurityContextHolder.getContext().getAuthentication())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return ret != null ? ret
						: new NotAuthorizedResult(getCurrentUser(null), SecurityMessageCodes.LOGIN_REQUIRED);
			}
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return ret != null ? ret
					: new NotAuthorizedResult(getCurrentUser(null), SecurityMessageCodes.ACCESS_DENIED);
		}

		CurrentUserNotFoundException cunfe = ExceptionUtils.findExceptionOfType(ex, CurrentUserNotFoundException.class);
		if (cunfe != null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return ret != null ? ret
					: new NotAuthorizedResult(getCurrentUser(null), SecurityMessageCodes.LOGIN_REQUIRED);
		}

		// TODO: Do we really need to send whole stack trace to client ??? I think we
		// should do it only during development
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		return new GenericServerErrorResult(exceptionTranslator.buildUserMessage(ex, LocaleContextHolder.getLocale()),
				new ExceptionInfo(ex));
	}

	protected String getCurrentUser(Authentication optionalAuthentication) {
		Authentication auth = optionalAuthentication;
		if (auth == null) {
			auth = SecurityContextHolder.getContext().getAuthentication();
		}

		if (auth != null) {
			return auth.getName();
		}

		return SecurityMessageCodes.ANONYMOUS;
	}

	public AuthenticationTrustResolver getAuthenticationTrustResolver() {
		return authenticationTrustResolver;
	}

	public void setAuthenticationTrustResolver(AuthenticationTrustResolver authenticationTrustResolver) {
		this.authenticationTrustResolver = authenticationTrustResolver;
	}

	public ExceptionTranslator getExceptionTranslator() {
		return exceptionTranslator;
	}

	@Autowired
	public void setExceptionTranslator(ExceptionTranslator exceptionTranslator) {
		this.exceptionTranslator = exceptionTranslator;
	}
}