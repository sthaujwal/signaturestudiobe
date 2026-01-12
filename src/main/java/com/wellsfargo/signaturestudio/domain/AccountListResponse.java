package com.wellsfargo.signaturestudio.domain;

import java.util.List;

/**
 * Response DTO for paginated account lists.
 */
public class AccountListResponse {

    private List<AccountSummary> accounts;
    private int totalCount;
    private int page;
    private int pageSize;

    public AccountListResponse() {
    }

    public AccountListResponse(List<AccountSummary> accounts, int totalCount) {
        this.accounts = accounts;
        this.totalCount = totalCount;
    }

    public AccountListResponse(List<AccountSummary> accounts, int totalCount, int page, int pageSize) {
        this.accounts = accounts;
        this.totalCount = totalCount;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<AccountSummary> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountSummary> accounts) {
        this.accounts = accounts;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
