package com.wellsfargo.signaturestudio.domain;

import java.util.List;

/**
 * Represents a permission for a specific operation/endpoint.
 * Used by the RolePermissionRegistry to track which operations require which roles.
 */
public class OperationPermission {

    private String httpMethod;           // GET, POST, PUT, DELETE, PATCH
    private String endpoint;              // /api/admin/settings/account
    private String controllerClass;       // AdminController
    private String methodName;            // updateAccountSettings
    private String description;           // "Update account settings"
    private List<String> requiredRoles;   // [ADMIN, ORG_ADMIN]
    private boolean requiresOrgAdmin;     // true if @RequireOrgAdmin is present

    public OperationPermission() {
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getControllerClass() {
        return controllerClass;
    }

    public void setControllerClass(String controllerClass) {
        this.controllerClass = controllerClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getRequiredRoles() {
        return requiredRoles;
    }

    public void setRequiredRoles(List<String> requiredRoles) {
        this.requiredRoles = requiredRoles;
    }

    public boolean isRequiresOrgAdmin() {
        return requiresOrgAdmin;
    }

    public void setRequiresOrgAdmin(boolean requiresOrgAdmin) {
        this.requiresOrgAdmin = requiresOrgAdmin;
    }

    @Override
    public String toString() {
        return "OperationPermission{" +
                "httpMethod='" + httpMethod + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", controllerClass='" + controllerClass + '\'' +
                ", methodName='" + methodName + '\'' +
                ", description='" + description + '\'' +
                ", requiredRoles=" + requiredRoles +
                ", requiresOrgAdmin=" + requiresOrgAdmin +
                '}';
    }
}
