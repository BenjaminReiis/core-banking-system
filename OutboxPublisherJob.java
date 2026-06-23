package com.example.corebanking.outbox;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisherJob {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisherJob(OutboxRepository outboxRepository,
                              KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent event : events) {
            kafkaTemplate.send("transfer-events", event.getAggregateId(), event.getPayload());
            event.setStatus("PUBLISHED");
            event.setPublishedAt(LocalDateTime.now());
            outboxRepository.save(event);
        }
    }
}
