package com.hrflow.roles.dtos;

public class RoleSearchRequest {

    private String keyword;
    private int page = 0;
    private int size = 10;
    private String sortBy = "id";
    private String direction = "asc";

    // getters and setters

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(page, 0); }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = Math.max(Math.min(size, 100), 1); }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
}