package edu.study.model;

public class SettingsConfig {
    private double pxPerHour = 24;
    private int refreshSeconds = 45;
    private PersonalProfile profile = new PersonalProfile();
    private java.util.List<String> memories = new java.util.ArrayList<>();

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

    public java.util.List<String> getMemories() {
        return memories;
    }

    public void setMemories(java.util.List<String> memories) {
        this.memories = memories;
    }
}
