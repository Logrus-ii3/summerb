package org.summerb.approaches.springmvc.security.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.summerb.approaches.springmvc.controllers.ControllerBase;
import org.summerb.approaches.springmvc.security.SecurityConstants;
import org.summerb.approaches.springmvc.security.SecurityMessageCodes;
import org.summerb.approaches.springmvc.security.UserAccountChangeHadlersDefaultImpl;
import org.summerb.approaches.springmvc.security.apis.SecurityActionsUrlsProvider;
import org.summerb.approaches.springmvc.security.apis.SecurityViewNamesProvider;
import org.summerb.approaches.springmvc.security.apis.UsersServiceFacade;
import org.summerb.approaches.springmvc.security.dto.PasswordChangePm;
import org.summerb.approaches.springmvc.security.dto.PasswordResetPm;
import org.summerb.approaches.springmvc.security.dto.Registration;
import org.summerb.approaches.springmvc.utils.AbsoluteUrlBuilder;
import org.summerb.approaches.springmvc.utils.CaptchaController;
import org.summerb.approaches.springmvc.utils.ErrorUtils;
import org.summerb.approaches.validation.FieldValidationException;
import org.summerb.approaches.validation.ValidationError;
import org.summerb.microservices.users.api.PermissionService;
import org.summerb.microservices.users.api.UserService;
import org.summerb.microservices.users.api.dto.User;
import org.summerb.microservices.users.api.exceptions.UserNotFoundException;
import org.summerb.utils.exceptions.ExceptionUtils;
import org.summerb.utils.exceptions.GenericException;

/**
 * This controller provides request/response-based actions for common
 * account-related operations
 * 
 * @author sergeyk
 *
 */
@Controller
public class LoginController extends ControllerBase {
	private Logger log = Logger.getLogger(getClass());

	private static final String ATTR_PASSWORD_RESET_TOKEN = "passwordResetToken";
	private static final String ATTR_REGISTERED = "registered";
	private static final String ATTR_ACTIVATED = "activated";
	private static final String ATTR_REGISTRATION = "registration";
	private static final String ATTR_PASSWORD_RESET_REQUEST = "resetPasswordRequest";
	private static final String ATTR_PASSWORD_RESET = "passwordReset";
	private static final String ATTR_RESET_OK = "resetOk";
	private static final String ATTR_PASSWORD_CHANGE = "passwordChange";
	private static final String ATTR_PASSWORD_CHANGED = "passwordChanged";

	@Autowired
	private UsersServiceFacade usersServiceFacade;
	@Autowired
	private UserService userService;
	@Autowired
	private PermissionService permissionService;
	@Autowired
	private AbsoluteUrlBuilder absoluteUrlBuilder;
	private RedirectStrategy redirectStrategy;
	private SecurityViewNamesProvider views;
	private SecurityActionsUrlsProvider securityActionsUrlsProvider;

	@Autowired(required = false)
	@Value("#{ props.properties['profile.dev'] }")
	private boolean isDevMode;

	@Autowired(required = false)
	@Value("#{ props.properties['profile.autotest'] }")
	private boolean isAutoTestMode;

	public LoginController() {
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (views == null) {
			views = new SecurityViewNamesProviderDefaultImpl();
		}

		if (securityActionsUrlsProvider == null) {
			securityActionsUrlsProvider = new SecurityActionsUrlsProviderDefaultImpl(views);
		}

		if (redirectStrategy == null) {
			redirectStrategy = new DefaultRedirectStrategy();
		}

		super.afterPropertiesSet();

		registerFirstUserIfNeeded();
	}

	// TODO: Remove this beore merging to submmerb
	// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	private void registerFirstUserIfNeeded() throws Exception {
		if (!isDevMode) {
			return;
		}

		String userEmail = "admin@site.ru";
		try {
			User user = userService.getUserByEmail(userEmail);
			if (!permissionService.hasPermission(SecurityConstants.DOMAIN, user.getUuid(), null,
					SecurityConstants.ROLE_ADMIN)) {
				permissionService.grantPermission(SecurityConstants.DOMAIN, user.getUuid(), null,
						SecurityConstants.ROLE_ADMIN);
			}
		} catch (UserNotFoundException nfe) {
			Registration r = new Registration();
			r.setEmail(userEmail);
			r.setDisplayName("Admin");
			r.setPassword("1111");
			User user = usersServiceFacade.registerUser(r);
			permissionService.grantPermission(SecurityConstants.DOMAIN, user.getUuid(), null,
					SecurityConstants.ROLE_ADMIN);
		}
	}

	@RequestMapping(method = RequestMethod.GET, value = SecurityActionsUrlsProviderDefaultImpl.URL_LOGIN_FORM)
	public String getLoginForm(Model model) {
		return views.loginForm();
	}

	@RequestMapping(method = RequestMethod.GET, value = SecurityActionsUrlsProviderDefaultImpl.URL_LOGIN_FAILED)
	public String handleLoginFailed(Model model, HttpServletRequest request) {
		Exception lastException = (Exception) request.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		if (lastException != null) {
			log.info("Login failed due to exception", lastException);
			model.addAttribute("lastExceptionMessage", ErrorUtils.getAllMessages(lastException));
		}

		model.addAttribute("loginError", true);

		// Add validation errors
		FieldValidationException validationErrors = ExceptionUtils.findExceptionOfType(lastException,
				FieldValidationException.class);
		if (validationErrors != null) {
			for (ValidationError error : validationErrors.getErrors()) {
				model.addAttribute("ve_" + error.getFieldToken(), msg(error.getMessageCode(), error.getMessageArgs()));
			}
		}

		// add login failed message
		return getLoginForm(model);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/login/invalid-session")
	public ModelAndView handleInvalidSession(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("requestedUrl") String requestedUrl) throws Exception {
		redirectStrategy.sendRedirect(request, response, requestedUrl);
		return null;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/login/register")
	public String getRegisterForm(Model model, HttpServletRequest request) {
		model.addAttribute(ATTR_REGISTRATION, new Registration());
		CaptchaController.putToken("register", request);
		return views.registerForm();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/login/register")
	public String processRegisterForm(@ModelAttribute(ATTR_REGISTRATION) Registration registration, Model model,
			HttpServletRequest request) throws FieldValidationException {
		if (!isAutoTestMode) {
			CaptchaController.assertCaptchaTokenValid("register", registration.getCaptcha(), request);
		}

		// Create user
		User user = usersServiceFacade.registerUser(registration);
		model.addAttribute(ATTR_REGISTERED, true);

		if (isDevMode) {
			String activationAbsoluteLink = absoluteUrlBuilder
					.buildExternalUrl(securityActionsUrlsProvider.buildRegistrationActivationPath(user.getUuid()));
			model.addAttribute(UserAccountChangeHadlersDefaultImpl.ATTR_ACTIVATION_LINK, activationAbsoluteLink);
		}

		return views.registerForm();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/login/activate")
	public String getRegistrationActivationForm(Model model, HttpServletRequest request,
			@RequestParam(value = SecurityActionsUrlsProviderDefaultImpl.PARAM_ACTIVATION_UUID) String activationUuid)
			throws GenericException {
		// Create user
		usersServiceFacade.activateRegistration(activationUuid);
		model.addAttribute(ATTR_ACTIVATED, true);

		return views.activateRegistration();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/login/request-reset")
	public String getPasswordResetRequestForm(Model model, HttpServletRequest request) {
		model.addAttribute(ATTR_PASSWORD_RESET_REQUEST, new Registration());
		CaptchaController.putToken("request-reset", request);
		return views.resetPasswordRequest();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/login/request-reset")
	public String processPasswordResetRequestForm(
			@ModelAttribute(ATTR_PASSWORD_RESET_REQUEST) Registration registration, Model model,
			HttpServletRequest request) throws FieldValidationException {
		if (!isAutoTestMode) {
			CaptchaController.assertCaptchaTokenValid("request-reset", registration.getCaptcha(), request);
		}

		String passwordResetToken = usersServiceFacade.getNewPasswordResetToken(registration.getEmail());

		// Generate registration link
		String passwordResetAbsoluteLink = absoluteUrlBuilder.buildExternalUrl(
				securityActionsUrlsProvider.buildPasswordResetPath(registration.getEmail(), passwordResetToken));
		if (isDevMode) {
			model.addAttribute(UserAccountChangeHadlersDefaultImpl.ATTR_PASSWORD_RESET_LINK, passwordResetAbsoluteLink);
		}
		return views.resetPasswordRequest();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/login/reset/{passwordResetToken}")
	public String getPasswordResetForm(@PathVariable(ATTR_PASSWORD_RESET_TOKEN) String passwordResetToken,
			@RequestParam(User.FN_EMAIL) String email, Model model, HttpServletRequest request)
			throws UserNotFoundException, FieldValidationException, GenericException {

		// Check if token valid
		if (!usersServiceFacade.isPasswordResetTokenValid(email, passwordResetToken)) {
			throw new GenericException(SecurityMessageCodes.INVALID_PASSWORD_RESET_TOKEN);
		}

		// Now let's show password reset form
		model.addAttribute(ATTR_PASSWORD_RESET, new PasswordResetPm());
		model.addAttribute(User.FN_EMAIL, email);
		model.addAttribute(ATTR_PASSWORD_RESET_TOKEN, passwordResetToken);

		return views.resetPassword();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/login/reset/{passwordResetToken}")
	public String processPasswordResetForm(
			@ModelAttribute(ATTR_PASSWORD_RESET_REQUEST) PasswordResetPm resetPasswordRequest,
			@PathVariable(ATTR_PASSWORD_RESET_TOKEN) String passwordResetToken,
			@RequestParam(User.FN_EMAIL) String email, Model model, HttpServletRequest request)
			throws UserNotFoundException, FieldValidationException {

		model.addAttribute(User.FN_EMAIL, email);
		model.addAttribute(ATTR_PASSWORD_RESET_TOKEN, passwordResetToken);
		model.addAttribute(ATTR_PASSWORD_RESET, resetPasswordRequest);

		usersServiceFacade.resetPassword(email, passwordResetToken, resetPasswordRequest);
		model.addAttribute(ATTR_RESET_OK, true);
		return views.resetPassword();
	}

	@Secured({ "ROLE_USER" })
	@RequestMapping(method = RequestMethod.GET, value = "/login/change")
	public String getPasswordChangeForm(Model model, HttpServletRequest request) {
		model.addAttribute(ATTR_PASSWORD_CHANGE, new PasswordChangePm());
		return views.changePassword();
	}

	@Secured({ "ROLE_USER" })
	@RequestMapping(method = RequestMethod.POST, value = "/login/change")
	public String processPasswordChangeForm(@ModelAttribute(ATTR_PASSWORD_CHANGE) PasswordChangePm passwordChangePm,
			Model model, HttpServletRequest request) throws UserNotFoundException, FieldValidationException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		usersServiceFacade.changePassword(auth.getName(), passwordChangePm);

		model.addAttribute(ATTR_PASSWORD_CHANGED, true);
		return views.changePassword();
	}

	public RedirectStrategy getRedirectStrategy() {
		return redirectStrategy;
	}

	@Autowired(required = false)
	public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
		this.redirectStrategy = redirectStrategy;
	}

	public SecurityViewNamesProvider getViews() {
		return views;
	}

	@Autowired(required = false)
	public void setViews(SecurityViewNamesProvider views) {
		this.views = views;
	}

	public SecurityActionsUrlsProvider getSecurityActionsUrlsProvider() {
		return securityActionsUrlsProvider;
	}

	@Autowired(required = false)
	public void setSecurityActionsUrlsProvider(SecurityActionsUrlsProvider securityActionsUrlsProvider) {
		this.securityActionsUrlsProvider = securityActionsUrlsProvider;
	}
}