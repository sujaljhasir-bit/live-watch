package com.cfs.livewatch.dto;

import com.cfs.livewatch.model.role;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Participantdto {
    private String id;
    private String username;

    @JsonProperty("role")   // always send it as "role"
    private role role;
}
