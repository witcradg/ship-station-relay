package io.witcradg.ordertrackingapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
public class EmailSenderServiceImpl implements IEmailSenderService {

	@Value("${spring.mail.properties.mail.smtp.from}")
	private String mailFrom;
	
	private JavaMailSender mailSender;

	public EmailSenderServiceImpl(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	@Override
	public void sendEmail(String to, String subject, String message) {
//		log.info("called sendEmail");
//		log.info(to);
//		log.info(subject);
//		log.info(message);

		SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
		simpleMailMessage.setFrom(mailFrom);
		simpleMailMessage.setTo(to);
		simpleMailMessage.setSubject(subject);
		simpleMailMessage.setText(message);

		this.mailSender.send(simpleMailMessage);
	}
}
