package com.generaltor.cloudfunction.webhook.entities;

public class LicenceKey {
    private String licenceKey;
    private boolean isDisabled;

    public LicenceKey(String licenceKey, boolean isDisabled) {
        this.licenceKey = licenceKey;
        this.isDisabled = isDisabled;
    }

    public String getLicenceKey() {
        return licenceKey;
    }

    public void setLicenceKey(String licenceKey) {
        this.licenceKey = licenceKey;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    public void setDisabled(boolean disabled) {
        isDisabled = disabled;
    }
}
