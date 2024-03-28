package com.generaltor.cloudfunction.webhook.entities;

public class Order {
    private String orderDate;
    private String lastResetDate;
    private Integer usageCount;
    private String licenceKey;
    private boolean isExpired;

    public Order(String orderDate, String lastResetDate, Integer usageCount, String licenceKey, boolean isExpired) {
        this.orderDate = orderDate;
        this.lastResetDate = lastResetDate;
        this.usageCount = usageCount;
        this.licenceKey = licenceKey;
        this.isExpired = isExpired;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getLastResetDate() {
        return lastResetDate;
    }

    public void setLastResetDate(String lastResetDate) {
        this.lastResetDate = lastResetDate;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public String getLicenceKey() {
        return licenceKey;
    }

    public void setLicenceKey(String licenceKey) {
        this.licenceKey = licenceKey;
    }

    public boolean isExpired() {
        return isExpired;
    }

    public void setExpired(boolean expired) {
        isExpired = expired;
    }
}
