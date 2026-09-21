package com.cfs.livewatch.repo;

import com.cfs.livewatch.model.room;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RoomRepository {

    private final Map<String, room> rooms = new ConcurrentHashMap<>();

    public room save(room room) {
        rooms.put(room.getCode(), room);
        return room;
    }

    public Optional<room> findCode(String code) {
        return Optional.ofNullable(rooms.get(code));
    }

    public boolean ExistsCode(String code) {
        return rooms.containsKey(code);
    }

    public void deleteCode(String code) {
        rooms.remove(code);
    }
}
