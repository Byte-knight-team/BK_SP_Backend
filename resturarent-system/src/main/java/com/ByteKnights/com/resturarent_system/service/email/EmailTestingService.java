package com.ByteKnights.com.resturarent_system.service.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailTestingService {

    // Runtime flag used by the email service
    private boolean forceFail;

    // Load initial default from application.properties
    public EmailTestingService(@Value("${app.email.force-fail:false}") boolean forceFail) {
        this.forceFail = forceFail;
    }

    public boolean isForceFail() {
        return forceFail;
    }

    public void setForceFail(boolean forceFail) {
        this.forceFail = forceFail;
    }
}