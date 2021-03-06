package org.summerb.approaches.springmvc.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.springmvc.security.apis.PasswordResetArmedHandler;
import org.summerb.approaches.springmvc.security.apis.SecurityActionsUrlsProvider;
import org.summerb.approaches.springmvc.security.apis.SecurityMailsMessageBuilderFactory;
import org.summerb.approaches.springmvc.security.apis.UserRegisteredHandler;
import org.summerb.approaches.springmvc.utils.AbsoluteUrlBuilder;
import org.summerb.microservices.emailsender.api.EmailSender;
import org.summerb.microservices.emailsender.api.dto.EmailMessage;
import org.summerb.microservices.emailsender.api.dto.EmailTemplateParams;
import org.summerb.microservices.users.api.dto.User;
import org.summerb.utils.exceptions.GenericRuntimeException;

public class UserAccountChangeHadlersDefaultImpl implements UserRegisteredHandler, PasswordResetArmedHandler {
	public static final String ATTR_PASSWORD_RESET_LINK = "resetPasswordLink";
	public static final String ATTR_ACTIVATION_LINK = "activationLink";

	@Autowired
	private EmailSender emailSender;
	@Autowired
	private SecurityMailsMessageBuilderFactory securityMailsMessageBuilderFactory;
	@Autowired
	private SecurityActionsUrlsProvider securityActionsUrlsProvider;
	@Autowired
	private AbsoluteUrlBuilder absoluteUrlBuilder;

	private boolean sendRegistrationConfirmationRequest = true;
	private boolean sendPasswordResetRequest = true;

	@Override
	public void onUserRegistered(User user) {
		try {
			if (user.getEmail().contains("@throw")) {
				throw new IllegalStateException("test throw on email failure - emulating no email sent");
			}
			if (!sendRegistrationConfirmationRequest) {
				return;
			}

			String senderEmail = securityMailsMessageBuilderFactory.getAccountOperationsSender().getEmail();
			EmailTemplateParams emailTemplateParams = new EmailTemplateParams(senderEmail, user, new Object());
			String activationAbsoluteLink = absoluteUrlBuilder
					.buildExternalUrl(securityActionsUrlsProvider.buildRegistrationActivationPath(user.getUuid()));
			emailTemplateParams.getExtension().put(ATTR_ACTIVATION_LINK, activationAbsoluteLink);
			EmailMessage emailMessage = securityMailsMessageBuilderFactory.getRegistrationEmailBuilder()
					.buildEmail(senderEmail, user.getEmail(), emailTemplateParams);
			emailSender.sendEmail(emailMessage);
		} catch (Throwable t) {
			throw new GenericRuntimeException(SecurityMessageCodes.FAILED_TO_SEND_REGISTRATION_EMAIL, t);
		}
	}

	@Override
	public void onPasswordResetRequested(User user, String passwordResetToken) {
		try {
			if (!sendPasswordResetRequest) {
				return;
			}

			String senderEmail = securityMailsMessageBuilderFactory.getAccountOperationsSender().getEmail();
			EmailTemplateParams emailTemplateParams = new EmailTemplateParams(senderEmail, user, new Object());
			String passwordResetAbsoluteLink = absoluteUrlBuilder.buildExternalUrl(
					securityActionsUrlsProvider.buildPasswordResetPath(user.getEmail(), passwordResetToken));
			emailTemplateParams.getExtension().put(ATTR_PASSWORD_RESET_LINK, passwordResetAbsoluteLink);
			EmailMessage emailMessage = securityMailsMessageBuilderFactory.getPasswordResetEmailBuilder()
					.buildEmail(senderEmail, user.getEmail(), emailTemplateParams);
			emailSender.sendEmail(emailMessage);
		} catch (Throwable t) {
			throw new GenericRuntimeException(SecurityMessageCodes.FAILED_TO_SEND_PASSWORD_REST_EMAIL, t);
		}
	}

	public boolean isSendRegistrationConfirmationRequest() {
		return sendRegistrationConfirmationRequest;
	}

	public void setSendRegistrationConfirmationRequest(boolean sendRegistrationConfirmationRequest) {
		this.sendRegistrationConfirmationRequest = sendRegistrationConfirmationRequest;
	}

	public boolean isSendPasswordResetRequest() {
		return sendPasswordResetRequest;
	}

	public void setSendPasswordResetRequest(boolean sendPasswordResetRequest) {
		this.sendPasswordResetRequest = sendPasswordResetRequest;
	}

}
