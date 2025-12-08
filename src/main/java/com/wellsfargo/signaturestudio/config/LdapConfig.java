package com.wellsfargo.signaturestudio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

/**
 * Configuration for LDAP/Active Directory connection.
 */
@Configuration
public class LdapConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(LdapConfig.class);
    
    @Value("${ldap.url}")
    private String ldapUrl;
    
    @Value("${ldap.base}")
    private String ldapBase;
    
    @Value("${ldap.username}")
    private String ldapUsername;
    
    @Value("${ldap.password}")
    private String ldapPassword;
    
    @Value("${ldap.user.search.base:}")
    private String userSearchBase;
    
    @Value("${ldap.user.search.filter:(&(objectClass=user)(|(sAMAccountName={0})(mail={0})(displayName=*{0}*)(cn=*{0}*)))}")
    private String userSearchFilter;
    
    @Bean
    public LdapContextSource ldapContextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapUrl);
        contextSource.setBase(ldapBase);
        contextSource.setUserDn(ldapUsername);
        contextSource.setPassword(ldapPassword);
        contextSource.setReferral("follow");
        
        try {
            contextSource.afterPropertiesSet();
            logger.info("LDAP context source configured successfully for URL: {}", ldapUrl);
        } catch (Exception e) {
            logger.error("Failed to configure LDAP context source", e);
            throw new RuntimeException("LDAP configuration failed", e);
        }
        
        return contextSource;
    }
    
    @Bean
    public LdapTemplate ldapTemplate(LdapContextSource ldapContextSource) {
        LdapTemplate ldapTemplate = new LdapTemplate(ldapContextSource);
        ldapTemplate.setIgnorePartialResultException(true);
        ldapTemplate.setIgnoreNameNotFoundException(true);
        return ldapTemplate;
    }
    
    public String getUserSearchBase() {
        return userSearchBase.isEmpty() ? ldapBase : userSearchBase;
    }
    
    public String getUserSearchFilter() {
        return userSearchFilter;
    }
}

