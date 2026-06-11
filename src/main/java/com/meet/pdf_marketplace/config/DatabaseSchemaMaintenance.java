package com.meet.pdf_marketplace.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class DatabaseSchemaMaintenance {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Keeps enum check constraints aligned with Java enums.
     * Hibernate ddl-auto=update does not rewrite existing enum check constraints.
     */
    @Bean
    public CommandLineRunner refreshEnumCheckConstraints() {

        return args -> jdbcTemplate.execute("""
                ALTER TABLE pdf_products
                DROP CONSTRAINT IF EXISTS pdf_products_status_check;

                ALTER TABLE pdf_products
                ADD CONSTRAINT pdf_products_status_check
                CHECK (status IN (
                    'DRAFT',
                    'PENDING_APPROVAL',
                    'PUBLISHED',
                    'UNPUBLISHED',
                    'REJECTED',
                    'DELETED'
                ));

                ALTER TABLE carts
                DROP CONSTRAINT IF EXISTS carts_status_check;

                ALTER TABLE carts
                ADD CONSTRAINT carts_status_check
                CHECK (status IN (
                    'ACTIVE',
                    'PAYMENT_PENDING',
                    'CHECKED_OUT',
                    'ABANDONED'
                ));
                """);
    }
}

