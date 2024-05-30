package dev.lydtech.dispatch.handler;

import dev.lydtech.dispatch.client.exception.NotRetryableException;
import dev.lydtech.dispatch.client.exception.RetryableException;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.service.DispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderCreatedHandler {

    private final DispatchService dispatchService;

    @KafkaListener( // this means Spring Kafka is responsible for polling Kafka for messages, and it will pass these to the relevant handlers annotated with the @KafkaListener annotation
            id = "orderConsumerClient",
            topics = "order.created",
            groupId = "dispatch.order.created.consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(@Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                       @Header(KafkaHeaders.RECEIVED_KEY) String key,
                       @Payload OrderCreated payload) {
        log.info("Payload received: parition: " + partition + " - key: " + key + " - payload: " + payload);
        try {
            dispatchService.process(key, payload);
        } catch (RetryableException e) {
            log.warn("Retryable exception: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("NotRetryable exception: " + e.getMessage());
            throw new NotRetryableException(e);
        }
    }
}
