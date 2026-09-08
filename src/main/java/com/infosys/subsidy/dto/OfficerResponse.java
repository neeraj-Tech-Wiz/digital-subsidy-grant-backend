package com.infosys.subsidy.dto;

import com.infosys.subsidy.entity.User;
import com.infosys.subsidy.enums.UserRole;

public class OfficerResponse {
    private Long id;
    private String name;
    private String email;
    private UserRole role;
    private boolean active;

    public OfficerResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.active = user.isActive();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
