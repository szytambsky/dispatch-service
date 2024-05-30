package dev.lydtech.dispatch.service;

import dev.lydtech.dispatch.client.StockServiceClient;
import dev.lydtech.dispatch.message.DispatchCompleted;
import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispatched;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.UUID.randomUUID;

@Slf4j
@Service
@AllArgsConstructor
public class DispatchService {

    private static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";
    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    private static final UUID APPLICATION_ID = randomUUID();
    private final KafkaTemplate<String, Object> kafkaProducer;
    private final StockServiceClient stockServiceClient;

    public void process(String key, OrderCreated orderCreated) throws Exception {
        String available = stockServiceClient.checkAvailability(orderCreated.getItem());
        if (Boolean.parseBoolean(available)) {
            DispatchPreparing dispatchPreparing = DispatchPreparing.builder()
                    .orderId(orderCreated.getOrderId())
                    .build();
            kafkaProducer.send(DISPATCH_TRACKING_TOPIC, key, dispatchPreparing).get();

            DispatchCompleted dispatchCompleted = DispatchCompleted.builder()
                    .orderId(orderCreated.getOrderId())
                    .date(LocalDateTime.now().toString())
                    .build();
            kafkaProducer.send(DISPATCH_TRACKING_TOPIC, key, dispatchCompleted).get();

            OrderDispatched orderDispatched = OrderDispatched.builder()
                    .orderId(orderCreated.getOrderId())
                    .processedById(APPLICATION_ID)
                    .notes("Dispatched: " + orderCreated.getItem())
                    .build();
            kafkaProducer.send(ORDER_DISPATCHED_TOPIC, key, orderDispatched).get();

            log.info("Sent messages: key: " + key + " - orderId: " + orderCreated.getOrderId() + " - processedById: " + APPLICATION_ID);
        } else {
            log.info("Item " + orderCreated.getItem() + " is unavailable.");
        }
    }
}
