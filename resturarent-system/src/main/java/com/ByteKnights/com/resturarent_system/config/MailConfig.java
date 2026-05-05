package com.ByteKnights.com.resturarent_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/*
    MailConfig is responsible for creating and configuring the JavaMailSender bean.
    JavaMailSender is used by the email service to send staff invitation emails,.
 */
@Configuration
public class MailConfig {

    /*
     * SMTP server host.
     */
    @Value("${spring.mail.host}")
    private String mailHost;

    /*
     * SMTP server port.
     */
    @Value("${spring.mail.port}")
    private int mailPort;

    /*
     * SMTP account username.
     */
    @Value("${spring.mail.username}")
    private String mailUsername;

    /*
     * SMTP account password or app password.
     */
    @Value("${spring.mail.password}")
    private String mailPassword;

    /*
     * Creates the JavaMailSender bean used by Spring to send emails.
     */
    @Bean
    public JavaMailSender javaMailSender() {

        /*
         * JavaMailSenderImpl is Spring's implementation of JavaMailSender.
         * It allows us to manually configure SMTP host, port, username, password, and additional mail properties.
         */
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        /*
         * Set the basic SMTP connection details.
         */
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);

        /*
         * Get the internal JavaMail properties object.
         * We use this to configure SMTP-specific behavior(authentication and STARTTLS encryption)
         */
        Properties props = mailSender.getJavaMailProperties();

        /*
         * SMTP is the standard mail transport protocol used for sending emails.
         */
        props.put("mail.transport.protocol", "smtp");

        /*
         * Enables SMTP authentication.
         * Required by most email providers (Gmail) because the server must verify the sender account.
         */
        props.put("mail.smtp.auth", "true");

        /*
         * Enables STARTTLS, upgrades the SMTP connection to a secure encrypted connection , common for port 587
         */
        props.put("mail.smtp.starttls.enable", "true");

        /*
         * Requires STARTTLS encryption, if the server does not support it, email sending will fail.
         */
        props.put("mail.smtp.starttls.required", "true");

        /*
         * Enables or disables SMTP debug logs, we use false unless debugging email issues.
         */
        props.put("mail.debug", "false");

        /*
         * Return the configured JavaMailSender bean.
         */
        return mailSender;
    }
}