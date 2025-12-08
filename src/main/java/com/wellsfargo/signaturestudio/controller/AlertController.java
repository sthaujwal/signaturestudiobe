package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.dto.AlertRequestDTO;
import com.wellsfargo.signaturestudio.dto.EmailTemplateDTO;
import com.wellsfargo.signaturestudio.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    
    private final AlertService alertService;
    
    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }
    
    @PostMapping("/send")
    public ResponseEntity<Void> sendAlert(@Valid @RequestBody AlertRequestDTO alertRequest) {
        alertService.sendAlert(alertRequest);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/templates")
    public ResponseEntity<List<EmailTemplateDTO>> getTemplates() {
        List<EmailTemplateDTO> templates = alertService.getTemplates();
        return ResponseEntity.ok(templates);
    }
    
    @GetMapping("/templates/{id}")
    public ResponseEntity<EmailTemplateDTO> getTemplate(@PathVariable String id) {
        EmailTemplateDTO template = alertService.getTemplate(id);
        return ResponseEntity.ok(template);
    }
}


