package com.dental.manage.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes the Twilio SDK with account credentials on startup.
 */
@Slf4j
@Configuration
public class TwilioConfig {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @PostConstruct
    public void initTwilio() {
        if (accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SDK initialized successfully");
        } else {
            log.warn("Twilio credentials not configured — SMS/WhatsApp features disabled");
        }
    }
}
