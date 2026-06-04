package com.notification.config;

import com.notification.event.OrderEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

/**
 * Kafka Consumer Configuration — Stream-Based Application
 *
 * Spring IoC: all beans registered via @Bean in @Configuration class.
 * ConcurrentKafkaListenerContainerFactory enables parallel partition consumption.
 */
@Configuration
@EnableKafka   // Spring IoC: activates @KafkaListener annotation processing
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Spring IoC: @Bean — ConsumerFactory<String, OrderEvent>
    @Bean
    public ConsumerFactory<String, OrderEvent> consumerFactory() {
        JsonDeserializer<OrderEvent> deserializer = new JsonDeserializer<>(OrderEvent.class, false);
        deserializer.addTrustedPackages("com.laundry.event", "com.notification.event");

        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,  bootstrapServers,
                        ConsumerConfig.GROUP_ID_CONFIG,            "notification-group",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,   "earliest",
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class,
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,  false  // manual ack for reliability
                ),
                new StringDeserializer(),
                deserializer
        );
    }

    // Spring IoC: @Bean — container factory with concurrency = 3 (one per partition)
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);  // parallel consumption across partitions
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
