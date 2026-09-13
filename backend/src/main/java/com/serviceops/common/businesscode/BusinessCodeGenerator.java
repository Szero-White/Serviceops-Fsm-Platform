package com.serviceops.common.businesscode;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

@Service
public class BusinessCodeGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String NEXT_VALUE_SQL = """
            insert into business_code_counters (tenant_id, code_type, business_date, last_value)
            values (?, ?, ?, 1)
            on conflict (tenant_id, code_type, business_date)
            do update set last_value = business_code_counters.last_value + 1
            returning last_value
            """;

    private final JdbcTemplate jdbcTemplate;
    private final BusinessCodeProperties properties;
    private final Clock clock;

    public BusinessCodeGenerator(
            JdbcTemplate jdbcTemplate,
            BusinessCodeProperties properties,
            @Qualifier(BusinessCodeConfiguration.CLOCK_BEAN) Clock clock
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(UUID tenantId, BusinessCodeType type) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(type, "type");

        LocalDate businessDate = LocalDate.now(clock.withZone(properties.zoneId()));
        Long sequence = jdbcTemplate.queryForObject(
                NEXT_VALUE_SQL,
                Long.class,
                tenantId,
                type.name(),
                businessDate
        );
        if (sequence == null) {
            throw new IllegalStateException("Không thể cấp mã nghiệp vụ mới");
        }
        return format(type, businessDate, sequence);
    }

    static String format(BusinessCodeType type, LocalDate businessDate, long sequence) {
        return "%s-%s-%03d".formatted(type.prefix(), DATE_FORMAT.format(businessDate), sequence);
    }
}
