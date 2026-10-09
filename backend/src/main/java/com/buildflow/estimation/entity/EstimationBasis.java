package com.buildflow.estimation.entity;

/**
 * What a rule's coefficient is expressed "per". FOUNDATION-type rules key off
 * GROUND_FLOOR_AREA (footprint) rather than TOTAL_BUILTUP_AREA, since a foundation doesn't scale
 * proportionally with extra floors the way framing, masonry, and finishes do.
 */
public enum EstimationBasis {
    TOTAL_BUILTUP_AREA,
    GROUND_FLOOR_AREA,
    FLOOR_COUNT,
    ROOM_COUNT,
    BATHROOM_COUNT,
    DOOR_COUNT,
    WINDOW_COUNT,
    FIXED,
    // The bases below come from a drawn floor plan; their rules apply only when every floor is drawn.
    EXTERNAL_WALL_AREA,
    INTERNAL_WALL_AREA,
    PLASTER_AREA,
    DRAWN_FLOOR_AREA,
    // From drawn columns, beams, slabs and footings; their rules replace the RCC area rules.
    STRUCTURAL_CONCRETE_M3,
    STRUCTURAL_STEEL_KG
}
