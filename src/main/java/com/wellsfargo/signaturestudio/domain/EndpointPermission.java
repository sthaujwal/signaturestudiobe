package com.wellsfargo.signaturestudio.domain;

import java.util.List;

/**
 * Represents the permission requirements for a specific endpoint.
 * Maps an endpoint to its allowed roles.
 */
public class EndpointPermission {

    private String endpoint;
    private String httpMethod;
    private List<String> allowedRoles;
    private boolean orgAdminOnly;
    private String description;

    public EndpointPermission() {
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public List<String> getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(List<String> allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public boolean isOrgAdminOnly() {
        return orgAdminOnly;
    }

    public void setOrgAdminOnly(boolean orgAdminOnly) {
        this.orgAdminOnly = orgAdminOnly;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "EndpointPermission{" +
                "endpoint='" + endpoint + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", allowedRoles=" + allowedRoles +
                ", orgAdminOnly=" + orgAdminOnly +
                ", description='" + description + '\'' +
                '}';
    }
}
