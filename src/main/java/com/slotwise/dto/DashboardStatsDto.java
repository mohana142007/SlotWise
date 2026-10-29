package com.slotwise.dto;

public class DashboardStatsDto {

    private long totalActiveResources;
    private long totalBookings;
    private long upcomingBookings;
    private long cancelledBookings;
    private long totalUsers;

    public DashboardStatsDto() {
    }

    public DashboardStatsDto(long totalActiveResources, long totalBookings, long upcomingBookings, long cancelledBookings, long totalUsers) {
        this.totalActiveResources = totalActiveResources;
        this.totalBookings = totalBookings;
        this.upcomingBookings = upcomingBookings;
        this.cancelledBookings = cancelledBookings;
        this.totalUsers = totalUsers;
    }

    public long getTotalActiveResources() {
        return totalActiveResources;
    }

    public void setTotalActiveResources(long totalActiveResources) {
        this.totalActiveResources = totalActiveResources;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public long getUpcomingBookings() {
        return upcomingBookings;
    }

    public void setUpcomingBookings(long upcomingBookings) {
        this.upcomingBookings = upcomingBookings;
    }

    public long getCancelledBookings() {
        return cancelledBookings;
    }

    public void setCancelledBookings(long cancelledBookings) {
        this.cancelledBookings = cancelledBookings;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }
}
