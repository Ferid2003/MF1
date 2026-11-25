package farid.aghazada.resource_service;

import java.util.concurrent.Executors;

import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ResourceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResourceServiceApplication.class, args);
	}

	// Built by hand, so Boot doesn't instrument it: pass the registry so service-to-service calls
	// are observed and carry the trace header (traceparent) to the next service.
	@Bean
	@LoadBalanced
	RestClient.Builder loadBalancedRestClientBuilder(ObservationRegistry observationRegistry) {
		return RestClient.builder().observationRegistry(observationRegistry);
	}

	// ponytail: @Primary plain builder so Eureka's own transport (getIfAvailable)
	// doesn't pick up the @LoadBalanced one and try to resolve "eureka-service" via LB.
	@Bean
	@Primary
	RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}

	// The circuit breaker runs calls on its own thread pool (so the TimeLimiter can cut them off).
	// Wrap that pool so each task carries the caller's trace context; otherwise the call to
	// storage-service would start a new trace instead of continuing the upload's one.
	@Bean
	Customizer<Resilience4JCircuitBreakerFactory> traceContextPropagation() {
		ContextSnapshotFactory snapshots = ContextSnapshotFactory.builder().build();
		return factory -> factory.configureExecutorService(
				ContextExecutorService.wrap(Executors.newCachedThreadPool(), snapshots::captureAll));
	}

}
