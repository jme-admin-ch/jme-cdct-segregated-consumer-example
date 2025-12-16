package ch.admin.bit.jeap.jme.cdct;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
@Slf4j
public class ConsumerApplication {

    static void main(String[] args) {
        Environment env = SpringApplication.run(ConsumerApplication.class, args).getEnvironment();
        String appName = env.getProperty("spring.application.name");
        String contextPath = env.getProperty("server.servlet.context-path");
        String serverPort = env.getProperty("server.port");


        log.info("""
                        ----------------------------------------------------------------------------
                        {}' is running!
                        
                        Profile(s): \t{}
                        SwaggerUI:  \thttp://localhost:{}{}/swagger-ui.html?urls.primaryName=public-api
                        
                        ---------------------------------------------------------------------------
                        """,
                appName, env.getActiveProfiles(), serverPort, contextPath);
    }
}
