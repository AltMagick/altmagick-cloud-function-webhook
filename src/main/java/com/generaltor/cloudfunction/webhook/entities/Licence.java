package com.generaltor.cloudfunction.webhook.entities;

public class Licence {
    private String licenceKey;
    private boolean isDisabled;

    public Licence(String licenceKey, boolean isDisabled) {
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
