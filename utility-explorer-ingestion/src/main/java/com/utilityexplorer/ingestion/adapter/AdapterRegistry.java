package com.utilityexplorer.ingestion.adapter;

import com.utilityexplorer.shared.adapter.IngestionAdapter;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AdapterRegistry {

    private final Map<String, IngestionAdapter> adapterMap = new HashMap<>();

    // Spring auto-magically injects all beans implementing IngestionAdapter here
    public AdapterRegistry(List<IngestionAdapter> adapters) {
        for (IngestionAdapter adapter : adapters) {
            adapterMap.put(adapter.getAdapterId(), adapter);
            System.out.println("Registered Ingestion Adapter: " + adapter.getAdapterId());
        }
    }

    public Optional<IngestionAdapter> getAdapter(String adapterId) {
        return Optional.ofNullable(adapterMap.get(adapterId));
    }
}
