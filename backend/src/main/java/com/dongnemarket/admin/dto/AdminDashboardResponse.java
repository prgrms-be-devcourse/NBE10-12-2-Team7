package com.dongnemarket.admin.dto;

/**
 * 관리 대시보드 집계 응답. 회원·상품·신고·댓글 현황을 한눈에 보여준다.
 */
public class AdminDashboardResponse {

    private final long totalMembers;
    private final long totalProducts;
    private final long totalReports;
    private final long pendingReports;
    private final long totalComments;

    private AdminDashboardResponse(long totalMembers, long totalProducts, long totalReports,
                                  long pendingReports, long totalComments) {
        this.totalMembers = totalMembers;
        this.totalProducts = totalProducts;
        this.totalReports = totalReports;
        this.pendingReports = pendingReports;
        this.totalComments = totalComments;
    }

    public static AdminDashboardResponse of(long totalMembers, long totalProducts, long totalReports,
                                           long pendingReports, long totalComments) {
        return new AdminDashboardResponse(totalMembers, totalProducts, totalReports, pendingReports, totalComments);
    }

    public long getTotalMembers() { return totalMembers; }
    public long getTotalProducts() { return totalProducts; }
    public long getTotalReports() { return totalReports; }
    public long getPendingReports() { return pendingReports; }
    public long getTotalComments() { return totalComments; }
}
