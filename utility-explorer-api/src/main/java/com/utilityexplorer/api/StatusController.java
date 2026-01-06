package com.utilityexplorer.api;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.service.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {
    
    @Autowired
    private StatusService statusService;
    
    @GetMapping("/sources")
    public List<SourceStatusResponse> getSourcesStatus() {
        return statusService.getSourcesStatus();
    }
}