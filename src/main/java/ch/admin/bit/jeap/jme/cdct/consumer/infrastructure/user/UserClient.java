package ch.admin.bit.jeap.jme.cdct.consumer.infrastructure.user;

import ch.admin.bit.jeap.security.restclient.JeapOAuth2RestClientBuilderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(JeapOAuth2RestClientBuilderFactory jeapOAuth2RestClientBuilderFactory, @Value("${user-api-url}") String taskApiUrl) {
        this.restClient = jeapOAuth2RestClientBuilderFactory.createForClientRegistryId("jme-cdct-segregated-consumer-service").baseUrl(taskApiUrl).build();
    }

    public User getUserById(String id) {
        return restClient.get()
                .uri("/{id}", id)
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.FORBIDDEN), (request, response) -> {
                    throw new InsufficientAuthenticationException("Insufficient authentication to access user API.");
                })
                .body(User.class);
    }

    @SuppressWarnings("java:S2583") // SonarQube seems to get this wrong, block can return null if the Mono is empty.
    public List<User> getAllUsers() {
        User[] userArray = restClient.get()
                .accept(APPLICATION_JSON)
                .retrieve()
                .body(User[].class);
        if (userArray != null) {
            return asList(userArray);
        } else {
            return emptyList();
        }
    }

}
