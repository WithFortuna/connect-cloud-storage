package com.example.aichatterdemo;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

public class GoogleDriveClientFactory {
    public static Drive create(String accessToken) {
        GoogleCredentials credentials = GoogleCredentials
                .create(new AccessToken(accessToken, null))
                .createScoped(DriveScopes.DRIVE_READONLY);
        HttpRequestInitializer httpRequestInitializer = new HttpCredentialsAdapter(credentials);
        return new Drive.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                httpRequestInitializer)
                .setApplicationName("MyB2BSolution")
                .build();
    }
}
