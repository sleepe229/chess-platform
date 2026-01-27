package com.chess.auth.config;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.ErrorListener;
import io.nats.client.Nats;
import io.nats.client.Options;
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

    @Value("${nats.connection-name:auth-service}")
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
                                case CONNECTED:
                                    log.info("NATS connection established: {}", natsUrl);
                                    break;
                                case DISCONNECTED:
                                    log.warn("NATS connection disconnected: {}", natsUrl);
                                    break;
                                case RECONNECTED:
                                    log.info("NATS connection reconnected: {}", natsUrl);
                                    break;
                                case CLOSED:
                                    log.warn("NATS connection closed: {}", natsUrl);
                                    break;
                                default:
                                    log.debug("NATS connection event: {}", type);
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
            return connection;
        } catch (IOException | InterruptedException e) {
            log.error("Failed to connect to NATS server: {}", natsUrl, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Failed to initialize NATS connection", e);
        }
    }
}
