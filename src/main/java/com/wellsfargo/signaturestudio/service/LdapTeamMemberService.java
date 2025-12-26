package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.config.LdapConfig;
import com.wellsfargo.signaturestudio.domain.TeamMember;
import com.wellsfargo.signaturestudio.domain.TeamMemberSearchRequest;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.List;

/**
 * Service for searching team members in Active Directory via LDAP.
 */
@Service
public class LdapTeamMemberService {
    
    private static final Logger logger = LoggerFactory.getLogger(LdapTeamMemberService.class);
    
    private final LdapTemplate ldapTemplate;
    private final LdapConfig ldapConfig;
    
    public LdapTeamMemberService(LdapTemplate ldapTemplate, LdapConfig ldapConfig) {
        this.ldapTemplate = ldapTemplate;
        this.ldapConfig = ldapConfig;
    }
    
    /**
     * Search for team members in Active Directory.
     * 
     * @param searchRequest Search request with query and max results
     * @return List of team members matching the search criteria
     */
    public List<TeamMember> searchTeamMembers(TeamMemberSearchRequest searchRequest) {
        String query = searchRequest.getQuery().trim();
        int maxResults = searchRequest.getMaxResults();
        
        logger.info("Searching for team members with query: '{}', maxResults: {}", query, maxResults);
        
        try {
            // Build LDAP search filter
            String searchFilter = buildSearchFilter(query);
            
            // Build LDAP query
            LdapQuery ldapQuery = LdapQueryBuilder.query()
                .base(ldapConfig.getUserSearchBase())
                .filter(searchFilter);
            
            // Execute search and map results
            List<TeamMember> teamMembers = ldapTemplate.search(ldapQuery, new TeamMemberAttributesMapper());
            
            // Limit results manually if needed
            if (teamMembers.size() > maxResults) {
                teamMembers = teamMembers.subList(0, maxResults);
            }
            
            logger.info("Found {} team members matching query: '{}'", teamMembers.size(), query);
            return teamMembers;
            
        } catch (Exception e) {
            logger.error("Error searching team members in LDAP with query: '{}'", query, e);
            throw new ServiceException(ErrorCode.INTERNAL_ERROR, 
                "Failed to search team members in Active Directory", e);
        }
    }
    
    /**
     * Builds LDAP search filter for Active Directory user search.
     * Searches only specific fields: username, email, first name, last name, and full name.
     * Uses prefix matching (starts with) for better performance and precision.
     */
    private String buildSearchFilter(String query) {
        // Escape special LDAP characters
        String escapedQuery = escapeLdapSearchFilter(query);
        
        // Build filter to search only in specific fields:
        // - sAMAccountName (username) - prefix match for performance
        // - mail (email) - prefix match for performance
        // - givenName (first name) - prefix match
        // - sn (last name) - prefix match
        // - displayName (full name) - prefix match
        // - cn (common name/full name) - prefix match
        // Using prefix matching (query*) instead of contains (*query*) for:
        // 1. Better performance (uses indexes)
        // 2. More predictable results
        // 3. Prevents overly broad searches
        // Only searches active users (excludes disabled accounts)
        return String.format(
            "(&(objectClass=user)(objectClass=person)(!(userAccountControl:1.2.840.113556.1.4.803:=2))" +
            "(|(sAMAccountName=%s*)(mail=%s*)(givenName=%s*)(sn=%s*)(displayName=%s*)(cn=%s*)))",
            escapedQuery, escapedQuery, escapedQuery, escapedQuery, escapedQuery, escapedQuery
        );
    }
    
    /**
     * Escapes special characters in LDAP search filter.
     */
    private String escapeLdapSearchFilter(String filter) {
        if (filter == null || filter.isEmpty()) {
            return filter;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filter.length(); i++) {
            char c = filter.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\5c");
                    break;
                case '*':
                    sb.append("\\2a");
                    break;
                case '(':
                    sb.append("\\28");
                    break;
                case ')':
                    sb.append("\\29");
                    break;
                case '\u0000':
                    sb.append("\\00");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
    
    /**
     * Attributes mapper to convert LDAP attributes to TeamMember.
     */
    private static class TeamMemberAttributesMapper implements AttributesMapper<TeamMember> {
        
        @Override
        public TeamMember mapFromAttributes(Attributes attributes) throws NamingException {
            TeamMember teamMember = new TeamMember();
            
            // sAMAccountName (username)
            teamMember.setUsername(getAttributeValue(attributes, "sAMAccountName"));
            
            // Email
            teamMember.setEmail(getAttributeValue(attributes, "mail"));
            
            // First name
            teamMember.setFirstName(getAttributeValue(attributes, "givenName"));
            
            // Last name
            teamMember.setLastName(getAttributeValue(attributes, "sn"));
            
            // Display name
            teamMember.setDisplayName(getAttributeValue(attributes, "displayName"));
            
            // Full name (cn or displayName)
            String fullName = getAttributeValue(attributes, "cn");
            if (fullName == null || fullName.isEmpty()) {
                fullName = teamMember.getDisplayName();
            }
            if (fullName == null || fullName.isEmpty()) {
                // Construct from first and last name
                String firstName = teamMember.getFirstName() != null ? teamMember.getFirstName() : "";
                String lastName = teamMember.getLastName() != null ? teamMember.getLastName() : "";
                fullName = (firstName + " " + lastName).trim();
            }
            teamMember.setFullName(fullName);
            
            // Department
            teamMember.setDepartment(getAttributeValue(attributes, "department"));
            
            // Title
            teamMember.setTitle(getAttributeValue(attributes, "title"));
            
            // Employee ID
            teamMember.setEmployeeId(getAttributeValue(attributes, "employeeID"));
            
            // Distinguished Name
            teamMember.setDistinguishedName(getAttributeValue(attributes, "distinguishedName"));
            
            return teamMember;
        }
        
        private String getAttributeValue(Attributes attributes, String attributeName) throws NamingException {
            if (attributes.get(attributeName) != null) {
                Object value = attributes.get(attributeName).get();
                return value != null ? value.toString() : null;
            }
            return null;
        }
    }
}

