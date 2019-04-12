package xyz.fz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import xyz.fz.util.NginxUtil;

@SpringBootApplication
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        ConfigurableApplicationContext context = application.run(args);
        LOGGER.info("spring boot web server startup @ " + context.getEnvironment().getProperty("server.port"));
        NginxUtil.start();
        Runtime.getRuntime().addShutdownHook(new Thread(NginxUtil::stop));
    }
}
