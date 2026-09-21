package com.cfs.livewatch.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
@Getter
public class partipant {
    private final String token = UUID.randomUUID().toString();
    private final String id = UUID.randomUUID().toString();
    private final String user;
@Setter
    private role Role;
public partipant(String user , role Role){
this.user=user;
this.Role=Role;
}
}
