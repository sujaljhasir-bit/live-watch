package com.cfs.livewatch.dto;

import com.cfs.livewatch.model.role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.management.relation.Role;
@Data
@NoArgsConstructor
    @AllArgsConstructor
public class Participantdto {
    private String id;
    private String username;
    private role Role;
}
