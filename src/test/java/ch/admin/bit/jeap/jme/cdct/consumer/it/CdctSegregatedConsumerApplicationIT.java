package ch.admin.bit.jeap.jme.cdct.consumer.it;

import ch.admin.bit.jeap.jme.cdct.ConsumerApplication;
import ch.admin.bit.jeap.jme.test.BootServiceSpringIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ActiveProfilesResolver;

import java.time.Duration;

import static ch.admin.bit.jeap.jme.test.BootServiceIntegrationTestBase.TestProfileResolver.isCI;

/**
 * This is an integration test for the CDCT Segregated Consumer Example itself.
 * For an example of how to implement a CDC Consumer Test, see TaskClientConsumerPactTest.
 */
@SpringBootTest(classes = ConsumerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles(resolver = CdctSegregatedConsumerApplicationIT.TestProfileResolver.class, inheritProfiles = false)
public class CdctSegregatedConsumerApplicationIT extends BootServiceSpringIntegrationTestBase {

    private static final String HEALTH_URL = "http://localhost:8081/jme-cdct-segregated-consumer-service/actuator/health/readiness";

    @Test
    void test() {
        waitForService(HEALTH_URL, Duration.ofMinutes(1));
    }

    public static class TestProfileResolver implements ActiveProfilesResolver {
        @SuppressWarnings("NullableProblems")
        @Override
        public String[] resolve(Class<?> ignore) {
            return isCI() ? new String[]{"local", "ci"} : new String[]{"local", "docker"};
        }
    }
}
