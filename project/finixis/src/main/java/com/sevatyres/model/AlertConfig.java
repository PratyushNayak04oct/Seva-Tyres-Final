package com.sevatyres.model;

import java.time.LocalDateTime;

/** Configurable SMS/Email alert campaign for Seva Tyres customers. */
public class AlertConfig {

    public enum Channel { EMAIL, SMS }

    private int id;
    private String name;
    private String messageTemplate;
    private Channel channel = Channel.EMAIL;
    private int intervalDays = 7;
    private int durationDays = 30;
    private boolean active = true;
    private LocalDateTime lastRun;

    public AlertConfig() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMessageTemplate() { return messageTemplate; }
    public void setMessageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getLastRun() { return lastRun; }
    public void setLastRun(LocalDateTime lastRun) { this.lastRun = lastRun; }

    @Override
    public String toString() { return name + " (" + channel + ", every " + intervalDays + " days)"; }
}
