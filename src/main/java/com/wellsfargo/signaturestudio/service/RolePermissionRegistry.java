package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.annotation.RequireOrgAdmin;
import com.wellsfargo.signaturestudio.annotation.RequireRole;
import com.wellsfargo.signaturestudio.domain.EndpointPermission;
import com.wellsfargo.signaturestudio.domain.OperationPermission;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Permission Registry Service
 *
 * Automatically scans all Spring controllers at startup to build a registry of:
 * - Which operations each role can perform
 * - Which roles are required for each endpoint
 *
 * This provides a single source of truth for permissions that can be queried
 * for UI rendering, API documentation, and compliance reporting.
 */
@Service
public class RolePermissionRegistry implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(RolePermissionRegistry.class);

    private ApplicationContext applicationContext;

    // Role -> List of operations mapping
    private final Map<String, List<OperationPermission>> roleOperationsMap = new ConcurrentHashMap<>();

    // Endpoint -> Permission requirements mapping
    private final Map<String, EndpointPermission> endpointPermissionsMap = new ConcurrentHashMap<>();

    // All discovered roles
    private final Set<String> allRoles = ConcurrentHashMap.newKeySet();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Scans all controllers at startup and builds the permission registry.
     */
    @PostConstruct
    public void buildRegistry() {
        logger.info("Building role permission registry...");

        long startTime = System.currentTimeMillis();
        int controllerCount = 0;
        int endpointCount = 0;

        // Get all beans with @RestController or @Controller annotations
        Map<String, Object> controllers = new HashMap<>();
        controllers.putAll(applicationContext.getBeansWithAnnotation(RestController.class));
        controllers.putAll(applicationContext.getBeansWithAnnotation(Controller.class));

        for (Map.Entry<String, Object> entry : controllers.entrySet()) {
            Object controllerBean = entry.getValue();
            if (controllerBean != null) {
                Class<?> controllerClass = AopUtils.getTargetClass(controllerBean);

                controllerCount++;
                endpointCount += scanController(controllerClass);
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        logger.info("Permission registry built successfully in {}ms", duration);
        logger.info("Summary: {} controllers scanned, {} endpoints registered, {} unique roles discovered",
                controllerCount, endpointCount, allRoles.size());
        logger.info("Discovered roles: {}", allRoles);

        // Log summary by role
        for (String role : allRoles) {
            List<OperationPermission> operations = roleOperationsMap.get(role);
            logger.debug("Role '{}' can perform {} operations", role, operations != null ? operations.size() : 0);
        }
    }

    /**
     * Scans a single controller class for permission annotations.
     *
     * @param controllerClass the controller class to scan
     * @return number of endpoints registered
     */
    private int scanController(Class<?> controllerClass) {
        int endpointCount = 0;

        // Get base path from class-level @RequestMapping
        String basePath = getBasePath(controllerClass);

        // Scan all methods
        for (Method method : controllerClass.getDeclaredMethods()) {
            OperationPermission permission = extractPermission(controllerClass, method, basePath);

            if (permission != null) {
                registerPermission(permission);
                endpointCount++;
            }
        }

        return endpointCount;
    }

    /**
     * Extracts the base path from class-level @RequestMapping annotation.
     */
    private String getBasePath(Class<?> controllerClass) {
        RequestMapping classMapping = controllerClass.getAnnotation(RequestMapping.class);

        if (classMapping != null && classMapping.value().length > 0) {
            return normalizePath(classMapping.value()[0]);
        }

        return "";
    }

    /**
     * Extracts permission information from a controller method.
     */
    private OperationPermission extractPermission(Class<?> controllerClass, Method method, String basePath) {
        // Check if method has any HTTP mapping annotation
        String httpMethod = null;
        String path = null;

        // Check for HTTP method annotations
        if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            httpMethod = "GET";
            path = getPathFromMapping(mapping.value(), mapping.path());
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            httpMethod = "POST";
            path = getPathFromMapping(mapping.value(), mapping.path());
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            PutMapping mapping = method.getAnnotation(PutMapping.class);
            httpMethod = "PUT";
            path = getPathFromMapping(mapping.value(), mapping.path());
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
            httpMethod = "DELETE";
            path = getPathFromMapping(mapping.value(), mapping.path());
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            PatchMapping mapping = method.getAnnotation(PatchMapping.class);
            httpMethod = "PATCH";
            path = getPathFromMapping(mapping.value(), mapping.path());
        } else if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            httpMethod = mapping.method().length > 0 ? mapping.method()[0].name() : "GET";
            path = getPathFromMapping(mapping.value(), mapping.path());
        }

        // Skip if no HTTP mapping found
        if (httpMethod == null || path == null) {
            return null;
        }

        // Build full endpoint path
        String fullPath = normalizePath(basePath + "/" + path);

        // Check for permission annotations
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        RequireOrgAdmin requireOrgAdmin = method.getAnnotation(RequireOrgAdmin.class);

        // Skip if no permission annotations
        if (requireRole == null && requireOrgAdmin == null) {
            return null;
        }

        // Build OperationPermission
        OperationPermission permission = new OperationPermission();
        permission.setHttpMethod(httpMethod);
        permission.setEndpoint(fullPath);
        permission.setControllerClass(controllerClass.getSimpleName());
        permission.setMethodName(method.getName());

        // Extract roles and description
        List<String> requiredRoles = new ArrayList<>();
        String description = "";

        if (requireOrgAdmin != null) {
            permission.setRequiresOrgAdmin(true);
            requiredRoles.add("ORG_ADMIN");
            description = requireOrgAdmin.operation();
        }

        if (requireRole != null) {
            String[] roles = requireRole.value();
            requiredRoles.addAll(Arrays.asList(roles));

            // Use operation description from RequireRole if present
            if (requireRole.operation() != null && !requireRole.operation().isEmpty()) {
                description = requireRole.operation();
            }
        }

        permission.setRequiredRoles(requiredRoles);
        permission.setDescription(description.isEmpty() ? generateDefaultDescription(httpMethod, fullPath) : description);

        return permission;
    }

    /**
     * Extracts path from mapping annotation value or path attributes.
     */
    private String getPathFromMapping(String[] value, String[] path) {
        if (value.length > 0) {
            return value[0];
        }
        if (path.length > 0) {
            return path[0];
        }
        return "";
    }

    /**
     * Normalizes a URL path by removing duplicate slashes and ensuring proper format.
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        // Remove duplicate slashes and trailing slashes
        String normalized = path.replaceAll("/+", "/");

        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        return normalized;
    }

    /**
     * Generates a default description based on HTTP method and path.
     */
    private String generateDefaultDescription(String httpMethod, String path) {
        String action = switch (httpMethod) {
            case "GET" -> "View";
            case "POST" -> "Create";
            case "PUT" -> "Update";
            case "DELETE" -> "Delete";
            case "PATCH" -> "Modify";
            default -> "Access";
        };

        return action + " " + path;
    }

    /**
     * Registers a permission in both role and endpoint maps.
     */
    private void registerPermission(OperationPermission permission) {
        // Add to role -> operations map
        for (String role : permission.getRequiredRoles()) {
            roleOperationsMap.computeIfAbsent(role, k -> new ArrayList<>()).add(permission);
            allRoles.add(role);
        }

        // Add to endpoint -> permission map
        String endpointKey = permission.getHttpMethod() + " " + permission.getEndpoint();

        EndpointPermission endpointPermission = new EndpointPermission();
        endpointPermission.setEndpoint(permission.getEndpoint());
        endpointPermission.setHttpMethod(permission.getHttpMethod());
        endpointPermission.setAllowedRoles(new ArrayList<>(permission.getRequiredRoles()));
        endpointPermission.setOrgAdminOnly(permission.isRequiresOrgAdmin());
        endpointPermission.setDescription(permission.getDescription());

        endpointPermissionsMap.put(endpointKey, endpointPermission);
    }

    // ==================== Query Methods ====================

    /**
     * Returns all operations that a specific role can perform.
     *
     * @param role the role name (e.g., "ADMIN", "ORG_ADMIN")
     * @return list of operations, or empty list if role not found
     */
    public List<OperationPermission> getOperationsForRole(String role) {
        return roleOperationsMap.getOrDefault(role, Collections.emptyList());
    }

    /**
     * Returns the complete permission matrix (all roles and their operations).
     *
     * @return map of role -> list of operations
     */
    public Map<String, List<OperationPermission>> getAllPermissions() {
        return new HashMap<>(roleOperationsMap);
    }

    /**
     * Returns all discovered roles.
     *
     * @return set of all role names
     */
    public Set<String> getAllRoles() {
        return new HashSet<>(allRoles);
    }

    /**
     * Returns permission requirements for a specific endpoint.
     *
     * @param httpMethod the HTTP method (GET, POST, etc.)
     * @param endpoint the endpoint path
     * @return endpoint permission, or null if not found
     */
    public EndpointPermission getEndpointPermission(String httpMethod, String endpoint) {
        String key = httpMethod + " " + normalizePath(endpoint);
        return endpointPermissionsMap.get(key);
    }

    /**
     * Returns all registered endpoints with their permission requirements.
     *
     * @return map of endpoint key -> permission requirements
     */
    public Map<String, EndpointPermission> getAllEndpointPermissions() {
        return new HashMap<>(endpointPermissionsMap);
    }

    /**
     * Checks if a specific role can access an endpoint.
     *
     * @param role the role to check
     * @param httpMethod the HTTP method
     * @param endpoint the endpoint path
     * @return true if role has access, false otherwise
     */
    public boolean canRoleAccessEndpoint(String role, String httpMethod, String endpoint) {
        EndpointPermission permission = getEndpointPermission(httpMethod, endpoint);

        if (permission == null) {
            // Endpoint not registered - assume no specific permission required
            return false;
        }

        return permission.getAllowedRoles().contains(role);
    }

    /**
     * Returns a summary of the registry for logging/debugging.
     *
     * @return human-readable summary
     */
    public String getRegistrySummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Permission Registry Summary:\n");
        summary.append("==========================\n");
        summary.append(String.format("Total Roles: %d\n", allRoles.size()));
        summary.append(String.format("Total Endpoints: %d\n", endpointPermissionsMap.size()));
        summary.append("\nRoles and Operation Counts:\n");

        for (String role : allRoles) {
            List<OperationPermission> operations = roleOperationsMap.get(role);
            summary.append(String.format("  - %s: %d operations\n", role, operations.size()));
        }

        return summary.toString();
    }
}
