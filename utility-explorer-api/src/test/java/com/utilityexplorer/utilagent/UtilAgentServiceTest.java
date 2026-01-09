package com.utilityexplorer.utilagent;

import com.utilityexplorer.dto.ApiDtos.UtilAgentResponse;
import com.utilityexplorer.persistence.UserQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UtilAgentServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UserQueryRepository userQueryRepository;
    
    // Mock other repos if needed: FactValueRepository, etc. but they are Autowired. 
    // In unit test @InjectMocks tries to fill them.
    // If they are null, execution might fail in 'fallback' path.
    // We strictly test the 'Intelligence Service' path here.

    @InjectMocks
    private UtilAgentService utilAgentService;

    @Test
    void processQuery_ShouldCallPythonService_WhenUrlConfigured() {
        // Given
        // Set intelligence URL via reflection
        org.springframework.test.util.ReflectionTestUtils.setField(
            utilAgentService, "intelligenceUrl", "http://mock-python:8000");

        // Mock RestTemplate response
        Map<String, Object> mockResponse = Map.of(
            "answer", "The price is $0.15",
            "sources", java.util.List.of()
        );
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
            .thenReturn(mockResponse);

        // When
        UtilAgentResponse response = utilAgentService.processQuery("What is the price?");

        // Then
        assertNotNull(response);
        assertEquals("The price is $0.15", response.getSummary());
        verify(restTemplate).postForObject(eq("http://mock-python:8000/query"), any(), eq(Map.class));
    }
}
