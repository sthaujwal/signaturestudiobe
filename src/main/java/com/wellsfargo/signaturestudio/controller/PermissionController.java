package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.annotation.RequireOrgAdmin;
import com.wellsfargo.signaturestudio.domain.OperationPermission;
import com.wellsfargo.signaturestudio.service.RolePermissionRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Permission Controller
 *
 * Provides REST APIs for querying the role-permission registry.
 * All endpoints require ORG_ADMIN privileges.
 *
 * Use cases:
 * - Dynamic UI rendering (show/hide features based on role)
 * - API discovery (what can a role do?)
 * - Compliance reporting (export permission matrix)
 * - Documentation generation
 */
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final RolePermissionRegistry permissionRegistry;

    public PermissionController(RolePermissionRegistry permissionRegistry) {
        this.permissionRegistry = permissionRegistry;
    }

    /**
     * Get all role permissions.
     *
     * Returns the complete permission matrix showing which operations
     * each role can perform.
     *
     * @return map of role -> list of operations
     */
    @GetMapping
    @RequireOrgAdmin(operation = "View all role permissions")
    public ResponseEntity<Map<String, List<OperationPermission>>> getAllPermissions() {
        Map<String, List<OperationPermission>> permissions = permissionRegistry.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }

    /**
     * Get permissions for a specific role.
     *
     * Returns all operations that the specified role can perform.
     * Useful for:
     * - Frontend UI rendering (show only allowed operations)
     * - Role capability documentation
     * - User onboarding (show what they can do)
     *
     * @param role the role name (e.g., "ADMIN", "ORG_ADMIN", "SENDER")
     * @return list of operations the role can perform
     */
    @GetMapping("/roles/{role}")
    @RequireOrgAdmin(operation = "View permissions for specific role")
    public ResponseEntity<List<OperationPermission>> getRolePermissions(@PathVariable String role) {
        List<OperationPermission> operations = permissionRegistry.getOperationsForRole(role);

        if (operations.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(operations);
    }

    /**
     * Get all discovered roles.
     *
     * Returns the list of all roles discovered during application startup
     * by scanning controller annotations.
     *
     * @return list of role names
     */
    @GetMapping("/roles")
    @RequireOrgAdmin(operation = "List all roles")
    public ResponseEntity<Set<String>> getAllRoles() {
        Set<String> roles = permissionRegistry.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * Export all permissions as CSV.
     *
     * Generates a CSV report of the complete permission matrix.
     * Useful for:
     * - Compliance audits
     * - Security reviews
     * - Documentation
     * - Offline analysis
     *
     * CSV Format:
     * Role,HTTP Method,Endpoint,Controller,Method,Description
     *
     * @return CSV content as string
     */
    @GetMapping(value = "/export", produces = "text/csv")
    @RequireOrgAdmin(operation = "Export role permissions as CSV")
    public ResponseEntity<String> exportPermissions() {
        StringBuilder csv = new StringBuilder();

        // CSV Header
        csv.append("Role,HTTP Method,Endpoint,Controller,Method,Description\n");

        // Get all permissions
        Map<String, List<OperationPermission>> allPermissions = permissionRegistry.getAllPermissions();

        // Build CSV rows
        for (Map.Entry<String, List<OperationPermission>> entry : allPermissions.entrySet()) {
            String role = entry.getKey();

            for (OperationPermission operation : entry.getValue()) {
                csv.append(escapeCsv(role)).append(",");
                csv.append(escapeCsv(operation.getHttpMethod())).append(",");
                csv.append(escapeCsv(operation.getEndpoint())).append(",");
                csv.append(escapeCsv(operation.getControllerClass())).append(",");
                csv.append(escapeCsv(operation.getMethodName())).append(",");
                csv.append(escapeCsv(operation.getDescription())).append("\n");
            }
        }

        // Set headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"role-permissions.csv\"");

        return new ResponseEntity<>(csv.toString(), headers, HttpStatus.OK);
    }

    /**
     * Get registry summary.
     *
     * Returns a human-readable summary of the permission registry
     * including total roles, endpoints, and per-role operation counts.
     *
     * Useful for:
     * - System health checks
     * - Debugging
     * - Quick overview of authorization structure
     *
     * @return summary text
     */
    @GetMapping(value = "/summary", produces = "text/plain")
    @RequireOrgAdmin(operation = "View permission registry summary")
    public ResponseEntity<String> getRegistrySummary() {
        String summary = permissionRegistry.getRegistrySummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Escapes a CSV field value by:
     * - Wrapping in quotes if it contains comma, quote, or newline
     * - Escaping existing quotes by doubling them
     *
     * @param value the value to escape
     * @return escaped CSV value
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        // Check if value needs escaping
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            // Escape quotes by doubling them
            String escaped = value.replace("\"", "\"\"");
            // Wrap in quotes
            return "\"" + escaped + "\"";
        }

        return value;
    }
}
