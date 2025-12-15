package edu.study.model;

public class SettingsConfig {
    private double pxPerHour = 24;
    private int refreshSeconds = 45;
    private PersonalProfile profile = new PersonalProfile();

    public double getPxPerHour() {
        return pxPerHour;
    }

    public void setPxPerHour(double pxPerHour) {
        this.pxPerHour = pxPerHour;
    }

    public int getRefreshSeconds() {
        return refreshSeconds;
    }

    public void setRefreshSeconds(int refreshSeconds) {
        this.refreshSeconds = refreshSeconds;
    }

    public PersonalProfile getProfile() {
        return profile;
    }

    public void setProfile(PersonalProfile profile) {
        this.profile = profile;
    }
}
