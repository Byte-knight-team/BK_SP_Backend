package com.ByteKnights.com.resturarent_system.service.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailTestingService {

    /*
     * Runtime testing flag
     * false -> email service tries to send real email
     * true  -> email service intentionally fails
     */
    private boolean forceFail;

    /*
        Loads the default value from application.properties.
    */
    public EmailTestingService(@Value("${app.email.force-fail:false}") boolean forceFail) {
        this.forceFail = forceFail;
    }

    /*
        Returns whether email sending should be forced to fail.
        SmtpEmailService checks this before sending the real email.
    */
    public boolean isForceFail() {
        return forceFail;
    }

    public void setForceFail(boolean forceFail) {
        this.forceFail = forceFail;
    }
}