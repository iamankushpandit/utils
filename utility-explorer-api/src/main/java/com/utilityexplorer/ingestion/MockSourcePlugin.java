package com.utilityexplorer.ingestion;

import com.utilityexplorer.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class MockSourcePlugin implements SourcePlugin {
    
    @Autowired
    private FactValueRepository factValueRepository;
    
    @Override
    public String getSourceId() {
        return "EIA";
    }
    
    @Override
    public SourceCheckResult checkForUpdates(SourceContext ctx) throws Exception {
        // Mock: always has updates for testing
        return new SourceCheckResult(true, "mock-token-" + ctx.now.toEpochMilli(), ctx.now);
    }
    
    @Override
    public IngestResult ingest(SourceContext ctx, SourceCheckResult check) throws Exception {
        // Mock: create deterministic fact for current month
        LocalDate now = LocalDate.now(ctx.clock);
        LocalDate periodStart = now.withDayOfMonth(1);
        LocalDate periodEnd = now.withDayOfMonth(now.lengthOfMonth());
        
        FactValue fact = new FactValue();
        fact.setMetricId("ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH");
        fact.setSourceId("EIA");
        fact.setGeoLevel("STATE");
        fact.setGeoId("99"); // Mock state
        fact.setPeriodStart(periodStart);
        fact.setPeriodEnd(periodEnd);
        fact.setValueNumeric(new BigDecimal("15.5"));
        fact.setRetrievedAt(ctx.now);
        fact.setSourcePublishedAt(check.sourcePublishedAt);
        fact.setIsAggregated(false);
        
        // Idempotent upsert
        factValueRepository.save(fact);
        
        return new IngestResult(1, UUID.randomUUID(), false);
    }
}