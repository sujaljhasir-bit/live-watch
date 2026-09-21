package com.cfs.livewatch.model;

public enum role {
 Host, Moderator, Participant;
 public boolean playbackcontrol(){
     return this==Host||this==Moderator;
 }
 public boolean changecontrol(){
     return this==Host||this==Moderator;
 }
    public boolean managecontrol(){
        return this==Host;
    }
}
