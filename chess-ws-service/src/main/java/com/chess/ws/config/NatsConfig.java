package com.chess.ws.config;

import com.chess.events.constants.NatsSubjects;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.ErrorListener;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Configuration
public class NatsConfig {

    @Value("${nats.url:nats://localhost:4222}")
    private String natsUrl;

    @Value("${nats.connection-name:ws-service}")
    private String connectionName;

    @Value("${nats.max-reconnects:-1}")
    private int maxReconnects;

    @Value("${nats.reconnect-wait-ms:2000}")
    private long reconnectWaitMs;

    @Bean
    @ConditionalOnProperty(name = "nats.enabled", havingValue = "true", matchIfMissing = true)
    public Connection natsConnection() {
        try {
            Options options = new Options.Builder()
                    .server(natsUrl)
                    .connectionName(connectionName)
                    .maxReconnects(maxReconnects)
                    .reconnectWait(Duration.ofMillis(reconnectWaitMs))
                    .connectionListener(new ConnectionListener() {
                        @Override
                        public void connectionEvent(Connection conn, Events type) {
                            switch (type) {
                                case CONNECTED -> log.info("NATS connection established: {}", natsUrl);
                                case DISCONNECTED -> log.warn("NATS connection disconnected: {}", natsUrl);
                                case RECONNECTED -> log.info("NATS connection reconnected: {}", natsUrl);
                                case CLOSED -> log.warn("NATS connection closed: {}", natsUrl);
                                default -> log.debug("NATS connection event: {}", type);
                            }
                        }
                    })
                    .errorListener(new ErrorListener() {
                        @Override
                        public void errorOccurred(Connection conn, String error) {
                            log.error("NATS error: {}", error);
                        }

                        @Override
                        public void exceptionOccurred(Connection conn, Exception exp) {
                            log.error("NATS exception occurred", exp);
                        }

                        @Override
                        public void slowConsumerDetected(Connection conn, io.nats.client.Consumer consumer) {
                            log.warn("NATS slow consumer detected: {}", consumer);
                        }
                    })
                    .build();

            Connection connection = Nats.connect(options);
            log.info("Connected to NATS server: {}", natsUrl);
            initializeStreams(connection);
            return connection;
        } catch (IOException | InterruptedException e) {
            log.error("Failed to connect to NATS server: {}", natsUrl, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Failed to initialize NATS connection", e);
        }
    }

    @Bean
    @ConditionalOnProperty(name = "nats.enabled", havingValue = "true", matchIfMissing = true)
    public JetStream jetStream(Connection connection) {
        try {
            return connection.jetStream();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JetStream", e);
        }
    }

    private void initializeStreams(Connection connection) {
        try {
            JetStreamManagement jsm = connection.jetStreamManagement();
            ensureStream(jsm, NatsSubjects.STREAM_GAME, new String[]{
                    NatsSubjects.GAME_STARTED,
                    NatsSubjects.GAME_MOVE_MADE,
                    NatsSubjects.GAME_FINISHED,
                    NatsSubjects.GAME_TIME_EXPIRED
            });
        } catch (Exception e) {
            log.warn("Failed to initialize JetStream streams (ws-service). NATS will still run without persistence.", e);
        }
    }

    private void ensureStream(JetStreamManagement jsm, String streamName, String[] subjects) {
        boolean exists;
        try {
            jsm.getStreamInfo(streamName);
            exists = true;
        } catch (Exception ignored) {
            exists = false;
        }
        if (exists) {
            return;
        }
        try {
            jsm.addStream(StreamConfiguration.builder()
                    .name(streamName)
                    .storageType(StorageType.File)
                    .subjects(subjects)
                    .build());
            log.info("Created JetStream stream: {} subjects={}", streamName, String.join(",", subjects));
        } catch (Exception e) {
            log.info("JetStream stream {} already exists (or creation raced): {}", streamName, e.getMessage());
        }
    }
}

