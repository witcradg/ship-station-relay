package io.witcradg.ordertrackingapi.service;

public interface IEmailSenderService {
	public abstract void sendEmail(String to, String subject, String message);
}
