package com.cs.ge.services.google;

import com.cs.ge.entites.Guest;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.List;

@Component
public class GoogleContactService {

    private static final List<String> SCOPES = List.of(PeopleServiceScopes.CONTACTS_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "zeeven-people-backend-ouath2.apps.googleusercontent.com.json";

    private static final String APPLICATION_NAME = "ZEEVEN";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";


    public List<Guest> fetchContacts() {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            final PeopleService service =
                    new PeopleService.Builder(HTTP_TRANSPORT, JSON_FACTORY, this.getCredentials(HTTP_TRANSPORT))
                            .setApplicationName(APPLICATION_NAME)
                            .build();

            
            // Request 10 connections.
            final ListConnectionsResponse response = service.people().connections()
                    .list("people/me")
                    .setPageSize(10)
                    .setPersonFields("names,emailAddresses")
                    .execute();

            // Print display name of connections if available.
            final List<Person> connections = response.getConnections();
            if (connections != null && connections.size() > 0) {
                for (final Person person : connections) {
                    final List<Name> names = person.getNames();
                    if (names != null && names.size() > 0) {
                        System.out.println("Name: " + person.getNames().get(0)
                                .getDisplayName());
                    } else {
                        System.out.println("No names available for connection.");
                    }
                }
            } else {
                System.out.println("No connections found.");
            }

        } catch (final GeneralSecurityException |
                IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        final InputStream in = new ClassPathResource("credential/" + CREDENTIALS_FILE_PATH).getInputStream();
        final GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        final LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8183).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public String getAuthorisations() {

        return null;
    }
}
