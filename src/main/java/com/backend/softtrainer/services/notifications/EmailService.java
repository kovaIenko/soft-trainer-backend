package com.backend.softtrainer.services.notifications;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Service
@Slf4j
public class EmailService {

  @Value("${mail.smtp.auth}")
  private String smtpAuth;

  @Value("${mail.smtp.starttls.enable}")
  private String startTls;

  @Value("${mail.smtp.host}")
  private String smtpHost;

  @Value("${mail.smtp.port}")
  private String smtpPort;

  @Value("${mail.username}")
  private String username;

  @Value("${mail.password}")
  private String password;

  @Value("${mail.recipients}")
  private String recipientList;

  public void sendEmail(String subject, String body) {
    try {
      List<String> recipients = Arrays.asList(recipientList.split(","));

      Properties props = new Properties();
      props.put("mail.smtp.auth", smtpAuth);
      props.put("mail.smtp.starttls.enable", startTls);
      props.put("mail.smtp.host", smtpHost);
      props.put("mail.smtp.port", smtpPort);

      Session session = Session.getInstance(props, new Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(username, password);
        }
      });

      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(username));
      for (String recipient : recipients) {
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient.trim()));
      }
      message.setSubject(subject);
      message.setText(body);

      Transport.send(message);
      log.info("✅ Email sent successfully to {}", recipients);
    } catch (MessagingException e) {
      log.error("❌Failed to send email", e);
    }
  }
}
