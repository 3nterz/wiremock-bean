package dev.nathan.wiremock.app;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Named;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Named
@ApplicationScoped
public class WiremockMockServer {

    private static String port;
    private Logger logger;
    private WireMockServer wm;
    private WireMockConfiguration wmc;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        logger = LoggerFactory.getLogger(WiremockMockServer.class);

        final String TMP_DIR = System.getProperty("java.io.tmpdir");
        createFoldersIfNotExisting(TMP_DIR);

        wmc = new WireMockConfiguration()
                .dynamicPort()
                .usingFilesUnderDirectory(TMP_DIR)
                .notifier(new Slf4jNotifier(true));
        wm = new WireMockServer(wmc);
        wm.start();

        final String WIREMOCK_PORT = String.valueOf(wm.port());
        port = WIREMOCK_PORT;
        System.setProperty("wiremock.port", WIREMOCK_PORT);
        logger.info("Starting Wiremock Server on port: " + WIREMOCK_PORT);

        wm.enableRecordMappings(new SingleRootFileSource(Paths.get(TMP_DIR, "mappings").toFile()),
                new SingleRootFileSource(Paths.get(TMP_DIR, "__files").toFile()));
        
        if (isWiremockRecording()) {
            logger.info("Starting recording on Wiremock Server with target URL: " + System.getProperty("wiremock.record.target.url"));
            wm.startRecording(getWirmockRecordingTargetUrl());
        }
    }

    public String getPort() {
        return port;
    }

    private boolean isWiremockRecording() {
        final String wiremockTargetUrl = System.getProperty("wiremock.record.target.url");
        final String wiremockRecord = System.getProperty("wiremock.record");
        logger.info("wiremock.record.target.url: " + wiremockTargetUrl);
        logger.info("wiremock.record: " + wiremockRecord);
        return "true".equals(wiremockRecord) &&
            wiremockTargetUrl != null && wiremockTargetUrl.isEmpty() == false;
    }

    private String getWirmockRecordingTargetUrl() {
        return System.getProperty("wiremock.record.target.url");
    }

    private void createFoldersIfNotExisting(final String TMP_DIR) {
        try {
            Files.createDirectory(Paths.get(TMP_DIR, "__files"));
        } catch (IOException e) {
            logger.error(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        try {
            Files.createDirectory(Paths.get(TMP_DIR, "mappings"));
        } catch (IOException e) {
            logger.error(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public void cleanUp(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        if ("true".equals(System.getProperty("wiremock.record"))) {
            logger.info("Stopping Recording on Wiremock Server");
            wm.stopRecording();
        }
        logger.info("Stopping Wiremock Server");
        wm.stop();
    }
}
