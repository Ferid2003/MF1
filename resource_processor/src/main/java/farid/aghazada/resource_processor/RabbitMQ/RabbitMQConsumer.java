package farid.aghazada.resource_processor.RabbitMQ;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import farid.aghazada.resource_processor.Service.Mp3MetadataExtractor;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RabbitMQConsumer {

    private final Mp3MetadataExtractor mp3MetadataExtractor;

    @RabbitListener(queues = "${rabbitmq.queue}")
    public void recieveMessage(String message) {
        mp3MetadataExtractor.processResource(message);
    }

}
