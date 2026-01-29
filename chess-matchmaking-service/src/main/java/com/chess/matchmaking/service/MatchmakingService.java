package com.chess.matchmaking.service;

import com.chess.event.MatchFoundEvent;
import com.chess.event.PlayerLeftQueueEvent;
import com.chess.event.PlayerSearchingOpponentEvent;
import com.chess.matchmaking.model.QueuedPlayer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStream;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PushSubscribeOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private static final int INITIAL_RATING_RANGE = 100;
    private static final int RATING_RANGE_INCREMENT = 50;
    private static final int MAX_RATING_RANGE = 500;
    private static final long RANGE_EXPANSION_INTERVAL_SECONDS = 10;

    @Value("${nats.stream.name:chess-events}")
    private String streamName;

    @Value("${nats.subject.player.searching:chess.matchmaking.player.searching}")
    private String playerSearchingSubject;

    @Value("${nats.subject.player.left:chess.matchmaking.player.left}")
    private String playerLeftSubject;

    @Value("${nats.subject.match.found:chess.matchmaking.match.found}")
    private String matchFoundSubject;

    private final JetStream jetStream;
    private final MatchmakingQueueService queueService;
    private final ObjectMapper objectMapper;

    // Хранит информацию о времени добавления игрока в очередь для расширения
    // диапазона
    private final Map<String, Long> playerQueueTimes = new ConcurrentHashMap<>();
    // Хранит текущий диапазон поиска для каждого игрока
    private final Map<String, Integer> playerRanges = new ConcurrentHashMap<>();
    // Хранит рейтинг игроков для быстрого доступа
    private final Map<String, Integer> playerRatings = new ConcurrentHashMap<>();

    private JetStreamSubscription playerSearchingSubscription;
    private JetStreamSubscription playerLeftSubscription;

    @PostConstruct
    public void init() throws IOException {
        // Подписываемся на события поиска соперника
        PushSubscribeOptions searchingOptions = PushSubscribeOptions.builder()
                .durable("matchmaking-service-searching")
                .build();

        playerSearchingSubscription = jetStream.subscribe(
                playerSearchingSubject,
                searchingOptions,
                this::handlePlayerSearchingEvent);
        log.info("Subscribed to NATS subject: {}", playerSearchingSubject);

        // Подписываемся на события выхода из очереди
        PushSubscribeOptions leftOptions = PushSubscribeOptions.builder()
                .durable("matchmaking-service-left")
                .build();

        playerLeftSubscription = jetStream.subscribe(
                playerLeftSubject,
                leftOptions,
                this::handlePlayerLeftEvent);
        log.info("Subscribed to NATS subject: {}", playerLeftSubject);
        log.info("Matchmaking scheduler will run every {} seconds", RANGE_EXPANSION_INTERVAL_SECONDS);
    }

    @PreDestroy
    public void cleanup() {
        if (playerSearchingSubscription != null) {
            try {
                playerSearchingSubscription.unsubscribe();
            } catch (Exception e) {
                log.error("Error unsubscribing from player searching NATS", e);
            }
        }
        if (playerLeftSubscription != null) {
            try {
                playerLeftSubscription.unsubscribe();
            } catch (Exception e) {
                log.error("Error unsubscribing from player left NATS", e);
            }
        }
    }

    /**
     * Обрабатывает событие поиска соперника.
     */
    private void handlePlayerSearchingEvent(Message msg) {
        try {
            String messageBody = new String(msg.getData(), StandardCharsets.UTF_8);
            PlayerSearchingOpponentEvent event = objectMapper.readValue(messageBody,
                    PlayerSearchingOpponentEvent.class);

            log.info("Received PlayerSearchingOpponentEvent: playerId={}, rating={}",
                    event.playerId(), event.rating());

            // Добавляем игрока в очередь
            queueService.addPlayerToQueue(event.playerId(), event.rating());

            // Инициализируем диапазон поиска и сохраняем рейтинг
            playerQueueTimes.put(event.playerId(), System.currentTimeMillis());
            playerRanges.put(event.playerId(), INITIAL_RATING_RANGE);
            playerRatings.put(event.playerId(), event.rating());

            // Подтверждаем обработку сообщения
            msg.ack();

            // Пытаемся сразу найти пару с начальным диапазоном
            processPlayerMatchmaking(event.playerId(), event.rating(), INITIAL_RATING_RANGE);

        } catch (Exception e) {
            log.error("Error processing PlayerSearchingOpponentEvent", e);
            msg.nak(); // Negative acknowledgment - сообщение будет обработано повторно
        }
    }

    /**
     * Обрабатывает событие выхода игрока из очереди.
     */
    private void handlePlayerLeftEvent(Message msg) {
        try {
            String messageBody = new String(msg.getData(), StandardCharsets.UTF_8);
            PlayerLeftQueueEvent event = objectMapper.readValue(messageBody, PlayerLeftQueueEvent.class);

            log.info("Received PlayerLeftQueueEvent: playerId={}", event.playerId());

            // Удаляем игрока из очереди
            queueService.removePlayerFromQueue(event.playerId());

            // Удаляем из отслеживания
            playerQueueTimes.remove(event.playerId());
            playerRanges.remove(event.playerId());
            playerRatings.remove(event.playerId());

            // Подтверждаем обработку сообщения
            msg.ack();

        } catch (Exception e) {
            log.error("Error processing PlayerLeftQueueEvent", e);
            msg.nak(); // Negative acknowledgment - сообщение будет обработано повторно
        }
    }

    /**
     * Периодически обрабатывает матчмейкинг для всех игроков в очереди.
     */
    @Scheduled(fixedDelay = RANGE_EXPANSION_INTERVAL_SECONDS * 1000)
    public void processMatchmaking() {
        long currentTime = System.currentTimeMillis();

        // Обновляем диапазоны поиска для всех игроков
        playerQueueTimes.forEach((playerId, queueTime) -> {
            long timeInQueue = currentTime - queueTime;
            int expansions = (int) (timeInQueue / (RANGE_EXPANSION_INTERVAL_SECONDS * 1000));
            int newRange = Math.min(
                    INITIAL_RATING_RANGE + (expansions * RATING_RANGE_INCREMENT),
                    MAX_RATING_RANGE);

            Integer currentRange = playerRanges.get(playerId);
            if (currentRange == null || newRange > currentRange) {
                playerRanges.put(playerId, newRange);
                log.debug("Expanded search range for player {} to ±{}", playerId, newRange);
            }
        });

        // Пытаемся найти пары для всех игроков
        playerRanges.forEach((playerId, range) -> {
            Integer rating = playerRatings.get(playerId);
            if (rating != null) {
                processPlayerMatchmaking(playerId, rating, range);
            } else {
                // Игрок больше не в очереди
                playerQueueTimes.remove(playerId);
                playerRanges.remove(playerId);
                playerRatings.remove(playerId);
            }
        });
    }

    /**
     * Обрабатывает матчмейкинг для конкретного игрока.
     */
    private void processPlayerMatchmaking(String playerId, Integer rating, Integer range) {
        try {
            // Проверяем, что игрок все еще в очереди
            Double score = queueService.getPlayerRating(playerId);
            if (score == null) {
                // Игрок больше не в очереди
                playerQueueTimes.remove(playerId);
                playerRanges.remove(playerId);
                playerRatings.remove(playerId);
                return;
            }

            QueuedPlayer opponent = queueService.findMatchForPlayer(playerId, rating, range);
            if (opponent != null) {
                // Пара найдена!
                sendMatchFoundEvent(playerId, rating, opponent.getPlayerId(), opponent.getRating());

                // Удаляем обоих игроков из отслеживания
                playerQueueTimes.remove(playerId);
                playerRanges.remove(playerId);
                playerRatings.remove(playerId);
                playerQueueTimes.remove(opponent.getPlayerId());
                playerRanges.remove(opponent.getPlayerId());
                playerRatings.remove(opponent.getPlayerId());
            }
        } catch (Exception e) {
            log.error("Error processing matchmaking for player {}", playerId, e);
        }
    }

    /**
     * Отправляет событие о найденной паре.
     */
    private void sendMatchFoundEvent(String player1Id, Integer player1Rating,
            String player2Id, Integer player2Rating) {
        try {
            MatchFoundEvent event = new MatchFoundEvent(
                    player1Id, player1Rating,
                    player2Id, player2Rating);

            String eventJson = objectMapper.writeValueAsString(event);
            byte[] eventBytes = eventJson.getBytes(StandardCharsets.UTF_8);

            jetStream.publish(matchFoundSubject, eventBytes);
            log.info("Published MatchFoundEvent: {} (rating {}) vs {} (rating {})",
                    player1Id, player1Rating, player2Id, player2Rating);

        } catch (Exception e) {
            log.error("Error sending MatchFoundEvent", e);
            throw new RuntimeException("Failed to send match found event", e);
        }
    }
}
