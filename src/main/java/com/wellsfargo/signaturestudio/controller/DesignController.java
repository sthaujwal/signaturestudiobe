package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.config.DesignJwtValidationFilter;
import com.wellsfargo.signaturestudio.domain.DesignBootstrapRequest;
import com.wellsfargo.signaturestudio.domain.DesignBootstrapResponse;
import com.wellsfargo.signaturestudio.domain.DesignJwtClaims;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import com.wellsfargo.signaturestudio.service.DesignBootstrapService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/design")
public class DesignController {

    private final DesignBootstrapService designBootstrapService;

    public DesignController(DesignBootstrapService designBootstrapService) {
        this.designBootstrapService = designBootstrapService;
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<DesignBootstrapResponse> bootstrap(
        @Valid @RequestBody DesignBootstrapRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse) {
        Object claimsObj = httpRequest.getAttribute(DesignJwtValidationFilter.DESIGN_CLAIMS_REQUEST_ATTRIBUTE);
        if (!(claimsObj instanceof DesignJwtClaims claims)) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED, "Design JWT claims not available");
        }
        return ResponseEntity.ok(
            designBootstrapService.bootstrap(claims, request, httpRequest, httpResponse)
        );
    }
}
