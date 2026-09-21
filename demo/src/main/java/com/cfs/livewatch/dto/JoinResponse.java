package com.cfs.livewatch.dto;

import com.cfs.livewatch.model.role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinResponse {
    private String participantId;
    private String token;
    private role role;
    private RoomResponse room;

}
