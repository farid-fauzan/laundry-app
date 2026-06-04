package com.notification.kafka;

import com.notification.event.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer — Stream-Based Application
 *
 * Consumes OrderEvent messages from laundry-order-events topic.
 * In production: sends SMS/email/push notifications per event type.
 *
 * Spring IoC: @Component — registered as singleton bean,
 *             @KafkaListener activated by @EnableKafka in KafkaConsumerConfig.
 */
@Component
@Slf4j
public class OrderEventConsumer {

    @KafkaListener(
        topics     = "laundry-order-events",
        groupId    = "notification-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        try {
            log.info("▶ Received [partition={}, offset={}] eventType={} orderCode={}",
                    partition, offset, event.getEventType(), event.getOrderCode());

            switch (event.getEventType()) {
                case "ORDER_CREATED"        -> handleOrderCreated(event);
                case "ORDER_STATUS_UPDATED" -> handleStatusUpdated(event);
                case "ORDER_COMPLETED"      -> handleOrderCompleted(event);
                case "ORDER_CANCELLED"      -> handleOrderCancelled(event);
                default -> log.warn("Unknown event type: {}", event.getEventType());
            }

            ack.acknowledge();  // manual commit after successful processing

        } catch (Exception ex) {
            log.error("Failed to process event [{}] for order [{}]: {}",
                    event.getEventType(), event.getOrderCode(), ex.getMessage());
            // In production: send to DLQ (Dead Letter Queue) instead of re-throwing
        }
    }

    private void handleOrderCreated(OrderEvent event) {
        log.info("📦 [NOTIFICATION] New order received!");
        log.info("   Customer : {} ({})", event.getCustomerName(), event.getCustomerPhone());
        log.info("   Order    : {} | Service: {} | Total: Rp{}",
                event.getOrderCode(), event.getServiceType(), event.getTotalPrice());
        // TODO: send WhatsApp/SMS via Twilio or Fonnte
    }

    private void handleStatusUpdated(OrderEvent event) {
        log.info("🔄 [NOTIFICATION] Order status changed!");
        log.info("   Order  : {}", event.getOrderCode());
        log.info("   Status : {} → {}", event.getPreviousStatus(), event.getStatus());
        log.info("   Customer: {} ({})", event.getCustomerName(), event.getCustomerPhone());
        // TODO: push notification to customer
    }

    private void handleOrderCompleted(OrderEvent event) {
        log.info("✅ [NOTIFICATION] Order delivered!");
        log.info("   Order  : {} | Customer: {} ({})",
                event.getOrderCode(), event.getCustomerName(), event.getCustomerPhone());
        log.info("   Total paid: Rp{}", event.getTotalPrice());
        // TODO: send receipt + rating request
    }

    private void handleOrderCancelled(OrderEvent event) {
        log.info("❌ [NOTIFICATION] Order cancelled.");
        log.info("   Order  : {} | Customer: {} ({})",
                event.getOrderCode(), event.getCustomerName(), event.getCustomerPhone());
        // TODO: notify customer of cancellation
    }
}
