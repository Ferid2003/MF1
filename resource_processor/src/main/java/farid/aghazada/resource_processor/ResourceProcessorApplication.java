package farid.aghazada.resource_processor;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableResilientMethods
public class ResourceProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResourceProcessorApplication.class, args);
	}

	// Built by hand, so Boot doesn't instrument it: pass the registry so service-to-service calls
	// are observed and carry the trace header (traceparent) to the next service.
	@Bean
	@LoadBalanced
	RestClient.Builder loadBalancedRestClientBuilder(ObservationRegistry observationRegistry) {
		return RestClient.builder().observationRegistry(observationRegistry);
	}

	// ponytail: @Primary plain builder so Eureka's registry-fetch transport doesn't
	// pick up the @LoadBalanced one and try to resolve "eureka-service" via LB.
	@Bean
	@Primary
	RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}
}
