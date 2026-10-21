package farid.aghazada.resource_processor;

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

	@Bean
	@LoadBalanced
	RestClient.Builder loadBalancedRestClientBuilder() {
		return RestClient.builder();
	}

	// ponytail: @Primary plain builder so Eureka's registry-fetch transport doesn't
	// pick up the @LoadBalanced one and try to resolve "eureka-service" via LB.
	@Bean
	@Primary
	RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}
}
