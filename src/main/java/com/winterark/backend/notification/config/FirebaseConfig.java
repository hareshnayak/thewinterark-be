package com.winterark.backend.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @Value("${firebase.credentials.json:}")
    private String credentialsJson;

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try {
            InputStream serviceAccount = null;

            if (credentialsJson != null && !credentialsJson.trim().isEmpty()) {
                log.info("Initializing Firebase from inline JSON credentials...");
                serviceAccount = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
            } else if (credentialsPath != null && !credentialsPath.trim().isEmpty()) {
                Resource resource = credentialsPath.startsWith("classpath:")
                        ? new ClassPathResource(credentialsPath.substring("classpath:".length()))
                        : new FileSystemResource(credentialsPath);

                if (resource.exists()) {
                    log.info("Initializing Firebase from credentials file: {}", credentialsPath);
                    serviceAccount = resource.getInputStream();
                }
            } else {
                // Check if firebase-service-account.json exists in classpath
                ClassPathResource defaultResource = new ClassPathResource("firebase-service-account.json");
                if (defaultResource.exists()) {
                    log.info("Initializing Firebase from classpath:firebase-service-account.json...");
                    serviceAccount = defaultResource.getInputStream();
                }
            }

            if (serviceAccount != null) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                return FirebaseApp.initializeApp(options);
            } else {
                log.info("No Firebase service account credentials configured. Defaulting to application default credentials if available.");
                try {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.getApplicationDefault())
                            .build();
                    return FirebaseApp.initializeApp(options);
                } catch (Exception e) {
                    log.warn("Firebase credentials not supplied. FCM nudges will fallback to WebPush until serviceAccountKey.json is provided.");
                    return null;
                }
            }
        } catch (Exception e) {
            log.warn("Could not initialize Firebase Admin SDK: {}. Fallback active.", e.getMessage());
            return null;
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        if (firebaseApp != null) {
            return FirebaseMessaging.getInstance(firebaseApp);
        }
        return null;
    }
}
