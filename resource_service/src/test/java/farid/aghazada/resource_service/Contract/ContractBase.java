package farid.aghazada.resource_service.Contract;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.support.MessageBuilder;

import farid.aghazada.resource_service.RabbitMQ.RabbitMQProducer;

/**
 * Provider side of the messaging contract. Spring Cloud Contract 5.0 ships no AMQP verifier,
 * so the RabbitTemplate is replaced by a channel named after the exchange; the contract then
 * checks the exact payload and routing key the producer hands to the broker.
 */
@SpringBootTest(classes = ContractBase.Cfg.class, properties = {
        "rabbitmq.exchange=exchange-name",
        "rabbitmq.routing-key=routing-key"
})
@AutoConfigureMessageVerifier
public abstract class ContractBase {

    @Configuration
    @Import(RabbitMQProducer.class)
    static class Cfg {
        @Bean("exchange-name")
        QueueChannel exchangeChannel() {
            return new QueueChannel();
        }

        @Bean
        RabbitTemplate rabbitTemplate(QueueChannel exchangeChannel) {
            RabbitTemplate template = mock(RabbitTemplate.class);
            doAnswer(inv -> exchangeChannel.send(MessageBuilder.withPayload(inv.getArgument(2))
                    .setHeader("amqp_receivedRoutingKey", inv.getArgument(1))
                    .setHeader("contentType", "text/plain") // what SimpleMessageConverter sets for a String
                    .build()))
                    .when(template).convertAndSend(anyString(), anyString(), (Object) any());
            return template;
        }
    }

    @Autowired
    RabbitMQProducer producer;

    public void sendResourceUploaded() {
        producer.sendMessage("1");
    }
}
