package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.dto.TeamMemberDTO;
import com.wellsfargo.signaturestudio.dto.TeamMemberSearchRequestDTO;
import com.wellsfargo.signaturestudio.service.LdapTeamMemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for team member search functionality.
 * Searches Active Directory via LDAP for internal team members.
 */
@RestController
@RequestMapping("/api/team-members")
public class TeamMemberController {
    
    private final LdapTeamMemberService ldapTeamMemberService;
    
    public TeamMemberController(LdapTeamMemberService ldapTeamMemberService) {
        this.ldapTeamMemberService = ldapTeamMemberService;
    }
    
    /**
     * Search for team members in Active Directory.
     * 
     * @param searchRequest Search request containing query and max results
     * @return List of team members matching the search criteria
     */
    @PostMapping("/search")
    public ResponseEntity<List<TeamMemberDTO>> searchTeamMembers(
            @Valid @RequestBody TeamMemberSearchRequestDTO searchRequest) {
        List<TeamMemberDTO> teamMembers = ldapTeamMemberService.searchTeamMembers(searchRequest);
        return ResponseEntity.ok(teamMembers);
    }
    
    /**
     * Search for team members using query parameter (simpler GET endpoint).
     * 
     * @param query Search query (name, email, username)
     * @param maxResults Maximum number of results (default: 20, max: 50)
     * @return List of team members matching the search criteria
     */
    @GetMapping("/search")
    public ResponseEntity<List<TeamMemberDTO>> searchTeamMembersGet(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "20") Integer maxResults) {
        
        // Validate maxResults
        if (maxResults > 50) {
            maxResults = 50;
        }
        if (maxResults < 1) {
            maxResults = 20;
        }
        
        TeamMemberSearchRequestDTO searchRequest = new TeamMemberSearchRequestDTO(query, maxResults);
        List<TeamMemberDTO> teamMembers = ldapTeamMemberService.searchTeamMembers(searchRequest);
        return ResponseEntity.ok(teamMembers);
    }
}

