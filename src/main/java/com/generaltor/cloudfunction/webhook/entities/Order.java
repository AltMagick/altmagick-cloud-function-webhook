package com.generaltor.cloudfunction.webhook.entities;

import com.google.cloud.Timestamp;

public class Order {

    private boolean pause;
    private String status;
    private Timestamp endAt;
    private boolean cancelled;
    private Timestamp renewsAt;
    private Timestamp lastReset;
    private String userName;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String userEmail;
    private Integer usageCount;
    private Licence licence;

    public Order(boolean pause, String status, Timestamp endAt, boolean cancelled, Timestamp renewsAt, Timestamp lastReset, String userName, Timestamp createdAt, Timestamp updatedAt, String userEmail, Integer usageCount, Licence licence) {
        this.pause = pause;
        this.status = status;
        this.endAt = endAt;
        this.cancelled = cancelled;
        this.renewsAt = renewsAt;
        this.lastReset = lastReset;
        this.userName = userName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userEmail = userEmail;
        this.usageCount = usageCount;
        this.licence = licence;
    }

    public boolean isPause() {
        return pause;
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getEndAt() {
        return endAt;
    }

    public void setEndAt(Timestamp endAt) {
        this.endAt = endAt;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Timestamp getRenewsAt() {
        return renewsAt;
    }

    public void setRenewsAt(Timestamp renewsAt) {
        this.renewsAt = renewsAt;
    }

    public Timestamp getLastReset() {
        return lastReset;
    }

    public void setLastReset(Timestamp lastReset) {
        this.lastReset = lastReset;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public Licence getLicenceKey() {
        return licence;
    }

    public void setLicenceKey(Licence licence) {
        this.licence = licence;
    }
}
