package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.constants.SessionConstants;
import com.wellsfargo.signaturestudio.domain.DesignBootstrapRequest;
import com.wellsfargo.signaturestudio.domain.DesignBootstrapResponse;
import com.wellsfargo.signaturestudio.domain.DesignJwtClaims;
import com.wellsfargo.signaturestudio.domain.Transaction;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DesignBootstrapService {

    private final TransactionService transactionService;
    private final CsrfTokenService csrfTokenService;

    public DesignBootstrapService(TransactionService transactionService,
                                  CsrfTokenService csrfTokenService) {
        this.transactionService = transactionService;
        this.csrfTokenService = csrfTokenService;
    }

    public DesignBootstrapResponse bootstrap(DesignJwtClaims claims,
                                             DesignBootstrapRequest request,
                                             HttpServletRequest httpRequest,
                                             HttpServletResponse httpResponse) {
        String transactionId = claims.getTransactionId();
        if (request.getTransactionId() != null
            && !request.getTransactionId().isBlank()
            && !request.getTransactionId().equals(transactionId)) {
            throw new ServiceException(ErrorCode.FORBIDDEN, "Request transactionId does not match token");
        }

        Transaction transaction = transactionService.getTransactionDetails(transactionId);
        validateAccountScope(claims.getAccountId(), transaction.getAccountId());
        validateAccountScope(request.getAccountId(), transaction.getAccountId());

        HttpSession session = httpRequest.getSession(true);
        hydrateSession(session, transaction, claims);

        String accessToken = null;
        Optional<String> existingToken = readTokenFromSession(session);
        if (existingToken.isPresent()) {
            accessToken = existingToken.get();
        } else {
            accessToken = java.util.UUID.randomUUID().toString();
            session.setAttribute("ACCESS_TOKEN", accessToken);
            session.setAttribute("TOKEN_CREATED_AT", java.time.Instant.now());
        }
        CsrfToken csrfToken = csrfTokenService.getOrGenerateToken(httpRequest, httpResponse);

        DesignBootstrapResponse response = new DesignBootstrapResponse();
        response.setAccessToken(accessToken);
        response.setSessionId(session.getId());
        response.setCsrfToken(csrfToken != null ? csrfToken.getToken() : null);
        response.setCsrfHeaderName(csrfToken != null ? csrfToken.getHeaderName() : "X-XSRF-TOKEN");
        response.setTransaction(transaction);
        return response;
    }

    private void validateAccountScope(String requestedAccountId, String transactionAccountId) {
        if (requestedAccountId == null || requestedAccountId.isBlank()) {
            return;
        }
        if (transactionAccountId == null || !requestedAccountId.equals(transactionAccountId)) {
            throw new ServiceException(
                ErrorCode.FORBIDDEN,
                "Requested account does not have access to this transaction"
            );
        }
    }

    private void hydrateSession(HttpSession session, Transaction transaction, DesignJwtClaims claims) {
        String username = firstNonBlank(
            claims.getSubject(),
            transaction.getCreatorUsername(),
            transaction.getCreatedBy()
        );

        session.setAttribute(SessionConstants.USERNAME, username);
        session.setAttribute(SessionConstants.AUTHENTICATED, true);
        session.setAttribute(SessionConstants.ACCOUNT_ID, transaction.getAccountId());
        session.setAttribute(SessionConstants.EMAIL, transaction.getCreatorEmail());
        session.setAttribute(SessionConstants.LAST_ACCESS_TIME, System.currentTimeMillis());
    }

    private Optional<String> readTokenFromSession(HttpSession session) {
        Object token = session.getAttribute("ACCESS_TOKEN");
        if (token instanceof String tokenValue && !tokenValue.isBlank()) {
            return Optional.of(tokenValue);
        }
        return Optional.empty();
    }

    private String firstNonBlank(String... values) {
        if (values == null || values.length == 0) {
            return "design-user";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "design-user";
    }
}
