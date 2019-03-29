package xyz.fz.runner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import xyz.fz.record.server.RecordServer;

@Component
public class RecordRunner implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        RecordServer.startInterceptNone();
    }
}
