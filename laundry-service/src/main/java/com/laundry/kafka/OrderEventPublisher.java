package com.laundry.kafka;

import com.laundry.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${laundry.kafka.topic.order-events}")
    private String orderEventsTopic;

    public void publish(OrderEvent event) {
        event.setEventId(UUID.randomUUID().toString());

        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(orderEventsTopic, event.getOrderCode(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event [{}] for order [{}]: {}",
                        event.getEventType(), event.getOrderCode(), ex.getMessage());
            } else {
                log.info("Published event [{}] for order [{}] → partition={}, offset={}",
                        event.getEventType(),
                        event.getOrderCode(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
