package ch.admin.bit.jeap.jme.cdct.consumer.web.api.gateway;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import ch.admin.bit.jeap.jme.cdct.consumer.infrastructure.user.User;
import ch.admin.bit.jeap.jme.cdct.consumer.infrastructure.user.UserClient;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.client.MockJeapOAuth2RestClientBuilderFactory;
import ch.admin.bit.jeap.security.test.client.configuration.JeapOAuth2IntegrationTestClientConfiguration;
import ch.admin.bit.jeap.security.test.jws.JwsBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonArrayMinLike;
import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonArrayMinMaxLike;
import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static au.com.dius.pact.consumer.dsl.Matchers.fromProviderState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * This class tests the user API.
 * This test class specifies this consumer's pact (contracts) for its provider and checks that the consumer
 * can interact successfully with a provider that complies to that pact (consumer tests).
 */
@PactConsumerTest
@PactTestFor(pactVersion = PactSpecVersion.V4)
@MockServerConfig(hostInterface = "localhost", port = "8888")
@Import(JeapOAuth2IntegrationTestClientConfiguration.class)
@SpringBootTest(properties = {
        "task-api-url=http://localhost:8888/jme-cdct-segregated-provider-service/api/task",
        "user-api-url=http://localhost:8888/jme-cdct-segregated-provider-service/api/user"
})
@SuppressWarnings({"ConstantConditions", "SpringJavaInjectionPointsAutowiringInspection", "SpringBootApplicationProperties", "unused"})
class UserClientConsumerPactTest {

    private static final String CONSUMER = "bit-jme-cdct-segregated-consumer-service";

    private static final String PROVIDER = "bit-jme-cdct-segregated-provider-service_user";

    private static final String API_PATH = "/jme-cdct-segregated-provider-service/api/user";

    private static final String ID_FIELD_NAME = "id";
    private static final String NAME_FIELD_NAME = "name";

    private static final String ID_EXAMPLE_VALUE = "3423489";
    private static final String NAME_EXAMPLE_VALUE = "test-name";

    private static final String USER_ID_PARAM_NAME = "user-id";

    @Autowired
    private UserClient userClient;

    @Autowired
    private MockJeapOAuth2RestClientBuilderFactory mockRestClientBuilderFactory;

    private String userReadToken;
    private String unrelatedRoleToken;

    @BeforeEach
    void init(@Autowired JwsBuilderFactory jwsBuilderFactory) {
        // We prepare access tokens with different authorizations to be used in the consumer tests
        SemanticApplicationRole semanticApplicationRole = SemanticApplicationRole.builder()
                .system("jme")
                .resource("user")
                .operation("read")
                .build();
        userReadToken = jwsBuilderFactory.createValidForFixedLongPeriodBuilder("userClient", JeapAuthenticationContext.SYS)
                .withUserRoles(semanticApplicationRole)
                .build().serialize();
        unrelatedRoleToken = jwsBuilderFactory.createValidForFixedLongPeriodBuilder("userClient", JeapAuthenticationContext.SYS).
                withUserRoles("some-unrelated-role").
                build().serialize();
    }

    // A @Pact annotated method specifies an interaction between a consumer and its provider using the Pact DSL.
    // In the interaction specified below the provider is asked for a user with user id '1' and then provides that user to
    // the consumer. A known user id is needed in this interaction because the id is a required part of the path used to
    // fetch the user from the provider's REST endpoint. The existence of a user wit id '1' is a precondition for the specified
    // interaction, i.e. the provider is required to be in the state "A user with user id '1' is present" before the interaction
    // can take place.
    @Pact(provider = PROVIDER, consumer = CONSUMER)
    private V4Pact requestUserWithFixedIdUserBeingPresentInteraction(PactBuilder builder) {
        final String path = API_PATH + "/1";
        return builder.given("A user with user id '1' is present").
                expectsToReceiveHttpInteraction("A GET request to " + path, httpInteractionBuilder -> httpInteractionBuilder.
                        withRequest(httpRequestBuilder -> httpRequestBuilder.
                                header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).
                                header(HttpHeaders.AUTHORIZATION, "Bearer " + userReadToken).
                                method("GET").
                                path(path)).
                        willRespondWith(httpResponseBuilder -> httpResponseBuilder.
                                status(200).
                                header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                                body(
                                        newJsonBody(o -> {
                                            o.stringValue(ID_FIELD_NAME, "1");
                                            o.stringType(NAME_FIELD_NAME, NAME_EXAMPLE_VALUE);
                                        }).
                                                build()))).
                toPact();
    }

    // A @PactTest annotated method executes a single consumer Pact test for the interaction defined by the referenced pact method.
    // The test will start a mock of the provider on port 8888. The mock provider will behave as defined by the pact method. The test
    // will add the interaction specification to a generated file representation of this consumer's pact with the provider. The pact file
    // can then be uploaded to the Pact broker.
    @Test
    @PactTestFor(pactMethod = "requestUserWithFixedIdUserBeingPresentInteraction")
    void testGetUserByIdWithFixedIdUserBeingPresent() {
        mockRestClientBuilderFactory.getAuthTokenProvider().setAuthToken(userReadToken);

        User result = userClient.getUserById("1");

        assertUserValues(result, "1", NAME_EXAMPLE_VALUE);
    }


    // The User provider might not be able to create a user with a fixed user id to be used for a provider state, e.g. because
    // the IDs of users are created by a database sequence. Fortunately it is possible with Pact to dynamically assign parameters like
    // e.g. 'user-id' to a provider state and reference those parameters in the pact e.g. within the method pathFromProviderState()
    // as '${user-id}'. The consumer side Pact test will replace the parameter 'user-id' with the given example values. For the
    // provider side Pact test, the parameter must be set by the method that creates the provider state.
    @Pact(provider = PROVIDER, consumer = CONSUMER)
    private V4Pact requestUserWithUserBeingPresentInteraction(PactBuilder builder) {
        final String userIdParamExpression = "${" + USER_ID_PARAM_NAME + "}";
        final String path = API_PATH + "/" + userIdParamExpression;
        return builder.given("A user is present").
                expectsToReceiveHttpInteraction("A request to " + path, httpInteractionBuilder -> httpInteractionBuilder.
                        withRequest(httpRequestBuilder -> httpRequestBuilder.
                                header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).
                                header(HttpHeaders.AUTHORIZATION, "Bearer " + userReadToken).
                                method("GET").
                                path(fromProviderState(path, API_PATH + "/" + ID_EXAMPLE_VALUE))).
                        willRespondWith(httpResponseBuilder -> httpResponseBuilder.
                                status(200).
                                header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                                body(
                                        newJsonBody(o -> {
                                            o.valueFromProviderState(ID_FIELD_NAME, userIdParamExpression, ID_EXAMPLE_VALUE);
                                            o.stringType(NAME_FIELD_NAME, NAME_EXAMPLE_VALUE);
                                        }).
                                                build()))).
                toPact();
    }

    @Test
    @PactTestFor(pactMethod = "requestUserWithUserBeingPresentInteraction")
    void testGetUserByIdWithUserBeingPresent() {
        mockRestClientBuilderFactory.getAuthTokenProvider().setAuthToken(userReadToken);

        User result = userClient.getUserById(ID_EXAMPLE_VALUE);

        assertUserValues(result, ID_EXAMPLE_VALUE, NAME_EXAMPLE_VALUE);
    }


    // In the previous interaction definition 'requestUserWithUserBeingPresentInteraction()' provider state parameters were
    // set by the provider. Provider state parameters can also be set by the consumer as part of the interaction definition
    // in the given(..) clause. Those parameters become then a part of the pact. On the provider side, the methods that set up
    // provider states containing such parameter definitions will receive those definitions as a parameter map and can use
    // the parameter values for the state set-up. This makes it possible to specify provider states that are to some
    // extent generic, if needed. In the real world, there would be no need for this in our example, though.
    @Pact(provider = PROVIDER, consumer = CONSUMER)
    private V4Pact requestUserWithGivenUserIdBeingPresentInteraction(PactBuilder builder) {
        final String userIdParamExpression = "${" + USER_ID_PARAM_NAME + "}";
        return builder.given("A user with user id " + userIdParamExpression + " is present", USER_ID_PARAM_NAME, ID_EXAMPLE_VALUE).
                expectsToReceiveHttpInteraction("A GET request to " + API_PATH + "/" + userIdParamExpression, httpInteractionBuilder -> httpInteractionBuilder.
                        withRequest(httpRequestBuilder -> httpRequestBuilder.
                                header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).
                                header(HttpHeaders.AUTHORIZATION, "Bearer " + userReadToken).
                                method("GET").
                                path(API_PATH + "/" + ID_EXAMPLE_VALUE)).
                        willRespondWith(httpResponseBuilder -> httpResponseBuilder.
                                status(200).
                                header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                                body(
                                        newJsonBody(o -> {
                                            o.stringValue(ID_FIELD_NAME, ID_EXAMPLE_VALUE);
                                            o.stringType(NAME_FIELD_NAME, NAME_EXAMPLE_VALUE);
                                        }).
                                                build()))).
                toPact();
    }

    @Test
    @PactTestFor(pactMethod = "requestUserWithGivenUserIdBeingPresentInteraction")
    void testGetUserByIdWithGivenUserIdBeingPresent() {
        mockRestClientBuilderFactory.getAuthTokenProvider().setAuthToken(userReadToken);

        User result = userClient.getUserById(ID_EXAMPLE_VALUE);

        assertUserValues(result, ID_EXAMPLE_VALUE, NAME_EXAMPLE_VALUE);
    }


    // The following interaction specification defines how insufficient authorization is expected to be handled.
    @Pact(provider = PROVIDER, consumer = CONSUMER)
    private V4Pact requestUserWithInsufficientAuthorizationInteraction(PactBuilder builder) {
        final String path = API_PATH + "/1";
        return builder.given("A user with user id '1' is present").
                expectsToReceiveHttpInteraction("A request to " + path + " with insufficient authorization", httpInteractionBuilder -> httpInteractionBuilder.
                        withRequest(httpRequestBuilder -> httpRequestBuilder.
                                header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).
                                header(HttpHeaders.AUTHORIZATION, "Bearer " + unrelatedRoleToken).
                                method("GET").
                                path(path)).
                        willRespondWith(httpResponseBuilder -> httpResponseBuilder.
                                status(403))).
                toPact();
    }

    @Test
    @PactTestFor(pactMethod = "requestUserWithInsufficientAuthorizationInteraction")
    void testGetUserInsufficientAuthorization() {
        mockRestClientBuilderFactory.getAuthTokenProvider().setAuthToken(unrelatedRoleToken);

        assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> userClient.getUserById("1"));
    }


    // This interaction specification gives an example of the Pact DSL for the case that the provider response is
    // an array instead of a JSON object as in the previous specifications.
    @Pact(provider = PROVIDER, consumer = CONSUMER)
    private V4Pact requestUsersWithUsersBeingPresentInteraction(PactBuilder builder) {
        return builder.given("Several users are present").
                expectsToReceiveHttpInteraction("A request to " + API_PATH, httpInteractionBuilder -> httpInteractionBuilder.
                        withRequest(httpRequestBuilder -> httpRequestBuilder.
                                header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).
                                header(HttpHeaders.AUTHORIZATION, "Bearer " + userReadToken).
                                method("GET").
                                path(API_PATH)).
                        willRespondWith(httpResponseBuilder -> httpResponseBuilder.
                                status(200).
                                header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                                body(
                                        newJsonArrayMinLike(2, a ->
                                                a.object(o -> {
                                                    o.stringType(ID_FIELD_NAME, ID_EXAMPLE_VALUE);
                                                    o.stringType(NAME_FIELD_NAME, NAME_EXAMPLE_VALUE);
                                                })).
                                                build()))).
                toPact();
    }

    @Test
    @PactTestFor(pactMethod = "requestUsersWithUsersBeingPresentInteraction")
    void testGetAllUsersWithUsersBeingPresent() {
        mockRestClientBuilderFactory.getAuthTokenProvider().setAuthToken(userReadToken);

        List<User> users = userClient.getAllUsers();

        assertThat(users).hasSize(2);
        users.forEach(
                user -> assertUserValues(user, ID_EXAMPLE_VALUE, NAME_EXAMPLE_VALUE)
        );
    }

    // Additional interactions should be specified here...


    @SuppressWarnings("SameParameterValue")
    private void assertUserValues(User user, String id, String name) {
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getName()).isEqualTo(name);
    }

}
