package dev.lydtech.dispatch.integration;

import dev.lydtech.dispatch.DispatchConfiguration;
import dev.lydtech.dispatch.message.DispatchCompleted;
import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispatched;
import dev.lydtech.dispatch.util.TestEventData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Slf4j
@SpringBootTest(classes = {DispatchConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@EmbeddedKafka(controlledShutdown = true)
public class DispatchOrderCreationTest {
    private final static String ORDER_CREATED_TOPIC = "order.created";
    private final static String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    private final static String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";

    @Autowired
    private KafkaTemplate kafkaTemplate;
    @Autowired
    private KafkaTestListener kafkaTestListener;
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Configuration
    static class TestConfig {
        @Bean
        public KafkaTestListener testListener() {
            return new KafkaTestListener();
        }
    }

    @KafkaListener(
            groupId = "KafkaIntegrationTest",
            topics = {DISPATCH_TRACKING_TOPIC, ORDER_DISPATCHED_TOPIC}
    )
    public static class KafkaTestListener {
        AtomicInteger dispatchPreparingCounter = new AtomicInteger(0);
        AtomicInteger dispatchCompletedCounter = new AtomicInteger(0);
        AtomicInteger orderDispatchedCounter = new AtomicInteger(0);

        @KafkaHandler
        void receiveDispatchPreparing(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload DispatchPreparing payload) {
            log.debug("Received DispatchPreparing: key " + key + " - payload: " + payload);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            dispatchPreparingCounter.incrementAndGet();
        }

        @KafkaHandler
        void receiveDispatchCompleted(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload DispatchCompleted payload) {
            log.debug("Received DispatchCompleted: key " + key + " - payload " + payload);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            dispatchCompletedCounter.incrementAndGet();
        }

        @KafkaHandler
        void receiveOrderDispatched(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload OrderDispatched payload) {
            log.debug("Received OrderDispatched: key " + key + " - payload: " + payload);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            orderDispatchedCounter.incrementAndGet();
        }
    }

    @BeforeEach
    public void setUp() {
        kafkaTestListener.orderDispatchedCounter.set(0);
        kafkaTestListener.dispatchPreparingCounter.set(0);

        registry.getListenerContainers().stream()
                .forEach(container ->
                        ContainerTestUtils.waitForAssignment(container,
                                container.getContainerProperties().getTopics().length
                                        * embeddedKafkaBroker.getPartitionsPerTopic()));
    }

    @Test
    public void testOrderDispatchFlow() throws Exception {
        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(randomUUID(), "my-test_item");
        String key = UUID.randomUUID().toString();
        sendMessage(ORDER_CREATED_TOPIC, key, orderCreated);
        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(() -> kafkaTestListener.dispatchPreparingCounter.get(), equalTo(1));
        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(() -> kafkaTestListener.dispatchCompletedCounter.get(), equalTo(1));
        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(() -> kafkaTestListener.orderDispatchedCounter.get(), equalTo(1));
    }

    private void sendMessage(String topic, String key, Object data) throws Exception {
        kafkaTemplate.send(MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.KEY, key)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build()).get();
    }
}
