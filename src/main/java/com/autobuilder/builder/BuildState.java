package com.autobuilder.builder;

public enum BuildState {
    IDLE,
    ANALYZING_SCHEMATIC,
    CHECKING_INVENTORY,
    GATHERING_MATERIALS,
    BUILDING,
    PAUSED,
    COMPLETED,
    FAILED
}
