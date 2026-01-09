package com.utilityexplorer.ingestion.config;

import com.utilityexplorer.shared.persistence.Metric;
import com.utilityexplorer.shared.persistence.MetricRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YamlConfigLoaderTest {

    @Mock
    private MetricRepository metricRepository;

    @InjectMocks
    private YamlConfigLoader yamlConfigLoader;

    @Test
    void loadConfig_parsesYamlAndUpsertsMetrics() throws Exception {
        // Arrange
        String yamlContent = """
                metrics:
                  - id: "TEST_METRIC"
                    name: "Test Metric"
                    category: "TEST"
                    ingestion:
                      adapter: "TEST_ADAPTER"
                """;
        Resource mockResource = new ByteArrayResource(yamlContent.getBytes());
        
        // Reflection to set private field @Value
        Field field = YamlConfigLoader.class.getDeclaredField("metricsConfig");
        field.setAccessible(true);
        field.set(yamlConfigLoader, mockResource);

        when(metricRepository.findById("TEST_METRIC")).thenReturn(Optional.empty());

        // Act
        yamlConfigLoader.loadConfig();

        // Assert
        verify(metricRepository, times(1)).save(any(Metric.class));
    }
}