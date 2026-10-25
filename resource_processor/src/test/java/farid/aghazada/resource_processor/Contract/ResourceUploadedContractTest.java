package farid.aghazada.resource_processor.Contract;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.StubTrigger;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties.StubsMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import farid.aghazada.resource_processor.RabbitMQ.RabbitMQConsumer;
import farid.aghazada.resource_processor.ResourceProcessorApplication;
import farid.aghazada.resource_processor.Service.Mp3MetadataExtractor;

/**
 * Consumer side of the messaging contract. The stub from resource_service emits the contract's message
 * on the channel named after the exchange; it is handed to the real RabbitMQConsumer, as the broker would.
 */
@SpringBootTest(classes = { ResourceProcessorApplication.class, ResourceUploadedContractTest.Cfg.class }, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.loadbalancer.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@AutoConfigureStubRunner(ids = "farid.aghazada:resource_service:+:stubs", stubsMode = StubsMode.LOCAL)
class ResourceUploadedContractTest {

    @Configuration
    static class Cfg {
        @Bean("exchange-name")
        DirectChannel exchangeChannel() {
            return new DirectChannel();
        }
    }

    @Autowired
    StubTrigger trigger;

    @Autowired
    @Qualifier("exchange-name")
    DirectChannel exchange;

    @Autowired
    RabbitMQConsumer consumer;

    @MockitoBean
    Mp3MetadataExtractor extractor;

    @BeforeEach
    void bridge() {
        exchange.subscribe(msg -> {
            Object p = msg.getPayload();
            consumer.recieveMessage(p instanceof byte[] b ? new String(b) : p.toString());
        });
    }

    @Test
    void resourceUploadedMessage_isProcessedById() {
        trigger.trigger("resource_uploaded");

        verify(extractor).processResource("1");
    }
}
