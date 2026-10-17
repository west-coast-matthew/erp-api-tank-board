package com.wc_matthew.demo.erp.tank_board;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Scans all of com.wc_matthew.demo.erp so erp-core-java's CoreConfiguration, services and
 * GlobalExceptionHandler are picked up alongside this service's own beans. CoreConfiguration
 * registers the core entity and repository packages; the tank board ones are added here.
 */
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@ComponentScan(basePackages = { "com.wc_matthew.demo.erp" })
@EntityScan(basePackages = { "com.wc_matthew.demo.erp.core.entity", "com.wc_matthew.demo.erp.tank_board.entity" })
@EnableJpaRepositories(basePackages = { "com.wc_matthew.demo.erp.tank_board.repo" })
public class ErpApiTankBoardApplication {

	public static void main(String[] args) {
		SpringApplication.run(ErpApiTankBoardApplication.class, args);
	}

}
