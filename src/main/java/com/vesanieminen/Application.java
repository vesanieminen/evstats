package com.vesanieminen;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationEngineConfiguration;
import com.vaadin.collaborationengine.LicenseEventHandler;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.theme.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@SpringBootApplication
@Theme(value = "evstats")
@Push
@JsModule("src/prefers-color-scheme.js")
public class Application implements AppShellConfigurator, VaadinServiceInitListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        LicenseEventHandler licenseEventHandler = licenseEvent -> {
            switch (licenseEvent.getType()) {
                case GRACE_PERIOD_STARTED:
                case LICENSE_EXPIRES_SOON:

                    LOGGER.warn(licenseEvent.getMessage());
                    break;
                case GRACE_PERIOD_ENDED:
                case LICENSE_EXPIRED:
                    LOGGER.error(licenseEvent.getMessage());
                    break;
                default:
                    LOGGER.error("Unknown error: " + licenseEvent.getMessage());
            }
        };
        CollaborationEngineConfiguration configuration = new CollaborationEngineConfiguration(
                licenseEventHandler);
        CollaborationEngine ce =
                CollaborationEngine.configure(event.getSource(),
                        configuration);
    }

}
