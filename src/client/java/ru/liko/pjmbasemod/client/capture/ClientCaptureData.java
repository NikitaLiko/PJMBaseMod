package ru.liko.pjmbasemod.client.capture;

import ru.liko.pjmbasemod.common.gamemode.ControlPointSnapshot;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientCaptureData {
    private static final Map<String, ControlPointSnapshot> POINTS = new HashMap<>();

    private ClientCaptureData() {}

    public static void applyFullSync(List<ControlPointSnapshot> snapshots) {
        POINTS.clear();
        for (ControlPointSnapshot snapshot : snapshots) {
            POINTS.put(snapshot.id(), snapshot);
        }
    }

    public static void updatePoint(ControlPointSnapshot snapshot) {
        POINTS.put(snapshot.id(), snapshot);
    }

    public static List<ControlPointSnapshot> getPoints() {
        return POINTS.values().stream()
                .sorted(Comparator.comparing(ControlPointSnapshot::id))
                .toList();
    }
}

