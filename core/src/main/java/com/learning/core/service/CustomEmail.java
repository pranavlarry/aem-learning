package com.learning.core.service;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.Map;

public interface CustomEmail {
    void sendEmail(String templatePath, String[] toIds, Map params, ResourceResolver resolver);
}
