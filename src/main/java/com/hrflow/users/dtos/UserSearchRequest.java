package com.hrflow.users.dtos;

public class UserSearchRequest {

    private String keyword;
    private Boolean enabled;
    private String role;
    private int page = 0;
    private int size = 10;
    private String sortBy = "id";
    private String direction = "asc";

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(page, 0); }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = Math.max(Math.min(size, 100), 1); }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
}