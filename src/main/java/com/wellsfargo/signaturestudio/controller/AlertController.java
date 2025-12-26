package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.domain.AlertRequest;
import com.wellsfargo.signaturestudio.domain.EmailTemplate;
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
    public ResponseEntity<Void> sendAlert(@Valid @RequestBody AlertRequest alertRequest) {
        alertService.sendAlert(alertRequest);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/templates")
    public ResponseEntity<List<EmailTemplate>> getTemplates() {
        List<EmailTemplate> templates = alertService.getTemplates();
        return ResponseEntity.ok(templates);
    }
    
    @GetMapping("/templates/{id}")
    public ResponseEntity<EmailTemplate> getTemplate(@PathVariable String id) {
        EmailTemplate template = alertService.getTemplate(id);
        return ResponseEntity.ok(template);
    }
}


