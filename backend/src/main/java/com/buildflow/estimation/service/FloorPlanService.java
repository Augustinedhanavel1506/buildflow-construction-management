package com.buildflow.estimation.service;

import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.estimation.dto.HouseRequirementResponse;
import com.buildflow.estimation.dto.PlanModels.BeamDto;
import com.buildflow.estimation.dto.PlanModels.ColumnDto;
import com.buildflow.estimation.dto.PlanModels.FloorPlanResponse;
import com.buildflow.estimation.dto.PlanModels.StructureMetrics;
import com.buildflow.estimation.dto.PlanModels.LayoutRequest;
import com.buildflow.estimation.dto.PlanModels.Metrics;
import com.buildflow.estimation.dto.PlanModels.OpeningDto;
import com.buildflow.estimation.dto.PlanModels.PlansResponse;
import com.buildflow.estimation.dto.PlanModels.RoomDto;
import com.buildflow.estimation.entity.FloorPlan;
import com.buildflow.estimation.entity.FloorRequirement;
import com.buildflow.estimation.entity.HouseRequirement;
import com.buildflow.estimation.entity.HouseRequirementStatus;
import com.buildflow.estimation.entity.OpeningKind;
import com.buildflow.estimation.entity.PlanColumn;
import com.buildflow.estimation.entity.PlanFurniture;
import com.buildflow.estimation.dto.PlanModels.FurnitureDto;
import com.buildflow.estimation.entity.PlanOpening;
import com.buildflow.estimation.entity.PlanRoom;
import com.buildflow.estimation.entity.RoomType;
import com.buildflow.estimation.repository.FloorPlanRepository;
import com.buildflow.estimation.repository.HouseRequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FloorPlanService {

    static final Set<String> FURNITURE_KINDS = Set.of("BED", "SOFA", "TABLE", "CHAIR", "WARDROBE", "COUNTER", "FRIDGE",
            "TV_UNIT", "TOILET", "BASIN", "CAR", "RUG", "PLANT", "SHRINE", "DESK", "TREE", "SHRUB", "FLOWER_BED", "BENCH");

    private static final int MAX_ROOMS = 40;
    private static final int MAX_OPENINGS = 200;
    private static final double MIN_ROOM_SIDE_FT = 2.5;
    // Tolerates editor rounding when a room sits exactly on the plot edge or against a neighbour.
    private static final double TOLERANCE_FT = 0.05;

    private final FloorPlanRepository floorPlanRepository;
    private final HouseRequirementRepository houseRequirementRepository;
    private final CurrentUserProvider currentUserProvider;

    public FloorPlanService(FloorPlanRepository floorPlanRepository,
                             HouseRequirementRepository houseRequirementRepository,
                             CurrentUserProvider currentUserProvider) {
        this.floorPlanRepository = floorPlanRepository;
        this.houseRequirementRepository = houseRequirementRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public PlansResponse get(Long requirementId) {
        return plansFor(findOwned(requirementId));
    }

    @Transactional(readOnly = true)
    public String exportDxf(Long requirementId) {
        PlansResponse plans = plansFor(findOwned(requirementId));
        if (plans.floors().stream().noneMatch(FloorPlanResponse::saved)) {
            throw new BadRequestException("Draw at least one floor plan before exporting a CAD file.");
        }
        return DxfExporter.export(plans);
    }

    // Used by the engineer review, which has already authorised access to the project.
    @Transactional(readOnly = true)
    public PlansResponse plansForProject(Long projectId) {
        return houseRequirementRepository.findByProjectId(projectId).map(this::plansFor).orElse(null);
    }

    @Transactional
    public FloorPlanResponse save(Long requirementId, int floorLevel, LayoutRequest layout) {
        HouseRequirement requirement = findOwned(requirementId);
        boolean floorExists = requirement.getFloors().stream().anyMatch(f -> f.getFloorLevel() == floorLevel);
        if (!floorExists) {
            throw new BadRequestException("This plan has no floor " + floorLevel + ". Add the floor to the checklist first.");
        }
        validate(requirement, layout);

        FloorPlan plan = floorPlanRepository.findByHouseRequirementIdAndFloorLevel(requirementId, floorLevel)
                .orElseGet(() -> {
                    FloorPlan created = new FloorPlan();
                    created.setHouseRequirement(requirement);
                    created.setFloorLevel(floorLevel);
                    return created;
                });

        // Replace in place so orphanRemoval deletes rooms and openings the user removed.
        plan.getRooms().clear();
        plan.getOpenings().clear();
        plan.getColumns().clear();
        plan.getFurniture().clear();
        for (RoomDto room : layout.rooms()) {
            PlanRoom entity = new PlanRoom();
            entity.setFloorPlan(plan);
            entity.setClientId(room.id());
            entity.setType(room.type());
            entity.setName(room.name());
            entity.setX(room.x());
            entity.setY(room.y());
            entity.setWidthFt(room.widthFt());
            entity.setDepthFt(room.depthFt());
            plan.getRooms().add(entity);
        }
        if (layout.columns() != null) {
            for (ColumnDto column : layout.columns()) {
                PlanColumn entity = new PlanColumn();
                entity.setFloorPlan(plan);
                entity.setClientId(column.id());
                entity.setX(column.x());
                entity.setY(column.y());
                entity.setWidthFt(column.widthFt());
                entity.setDepthFt(column.depthFt());
                plan.getColumns().add(entity);
            }
        }
        if (layout.furniture() != null) {
            for (FurnitureDto item : layout.furniture()) {
                PlanFurniture entity = new PlanFurniture();
                entity.setFloorPlan(plan);
                entity.setClientId(item.id());
                entity.setKind(item.kind());
                entity.setX(item.x());
                entity.setY(item.y());
                entity.setWidthFt(item.widthFt());
                entity.setDepthFt(item.depthFt());
                entity.setHeightFt(item.heightFt());
                entity.setRotationDeg(item.rotationDeg());
                entity.setColour(item.colour());
                plan.getFurniture().add(entity);
            }
        }
        if (layout.openings() != null) {
            for (OpeningDto opening : layout.openings()) {
                PlanOpening entity = new PlanOpening();
                entity.setFloorPlan(plan);
                entity.setClientId(opening.id());
                entity.setRoomClientId(opening.roomId());
                entity.setKind(opening.kind());
                entity.setSide(opening.side());
                entity.setOffsetFt(opening.offsetFt());
                entity.setWidthFt(opening.widthFt());
                plan.getOpenings().add(entity);
            }
        }
        return toResponse(floorPlanRepository.save(plan));
    }

    // Proposes layouts from the checklist without saving them.
    @Transactional(readOnly = true)
    public PlansResponse suggest(Long requirementId) {
        HouseRequirement requirement = findOwned(requirementId);
        boolean multiFloor = requirement.getFloors().size() > 1;
        // All floors share one footprint, sized from the largest, so the staircase lines up.
        double footprint = requirement.getFloors().stream()
                .mapToDouble(f -> f.getFloorAreaSqft().doubleValue()).max().orElse(0);
        double[] setbacks = SiteRulesService.effectiveSetbacks(requirement);
        List<FloorPlanResponse> floors = new ArrayList<>();
        for (FloorRequirement floor : requirement.getFloors()) {
            LayoutRequest layout = LayoutSuggester.suggest(
                    requirement.getPlotWidthFt().doubleValue(), requirement.getPlotLengthFt().doubleValue(),
                    new LayoutSuggester.Spec(floor.getFloorAreaSqft().doubleValue(), footprint, floor.getBedroomCount(),
                            floor.getBathroomCount(), floor.isHasKitchen(), floor.isHasHall(), floor.isHasBalcony(),
                            floor.isHasPoojaRoom(), multiFloor, setbacks[0], setbacks[1], setbacks[2], setbacks[3]));
            List<ColumnDto> columns = StructureGeometry.suggestGridColumns(layout.rooms());
            floors.add(responseFor(floor.getFloorLevel(), false, layout.rooms(),
                    layout.openings() == null ? List.of() : layout.openings(), columns, List.of()));
        }
        return new PlansResponse(requirement.getPlotWidthFt(), requirement.getPlotLengthFt(), floors);
    }

    // Columns for rooms the caller has drawn but not necessarily saved; nothing is stored.
    public List<ColumnDto> suggestColumns(Long requirementId, LayoutRequest layout, boolean everyJunction) {
        findOwned(requirementId);
        return everyJunction ? StructureGeometry.suggestColumns(layout.rooms()) : StructureGeometry.suggestGridColumns(layout.rooms());
    }

    /**
     * Brings the checklist in line with the saved drawings: area, room counts and openings are read
     * from the plan, so the estimate is driven by what was drawn. Floors without a drawing are left
     * as they are. The estimate becomes stale, so the requirement returns to DRAFT.
     */
    @Transactional
    public HouseRequirementResponse applyToChecklist(Long requirementId) {
        HouseRequirement requirement = findOwned(requirementId);
        List<FloorPlan> plans = floorPlanRepository.findByHouseRequirementIdOrderByFloorLevelAsc(requirementId);

        int applied = 0;
        for (FloorPlan plan : plans) {
            if (plan.getRooms().isEmpty()) {
                continue;
            }
            FloorRequirement floor = requirement.getFloors().stream()
                    .filter(f -> f.getFloorLevel() == plan.getFloorLevel())
                    .findFirst().orElse(null);
            if (floor == null) {
                continue;
            }
            Metrics m = metrics(toRoomDtos(plan), toOpeningDtos(plan));
            floor.setFloorAreaSqft(m.builtUpAreaSqft());
            floor.setBedroomCount(m.bedrooms());
            floor.setBathroomCount(m.bathrooms());
            floor.setHasKitchen(has(plan, RoomType.KITCHEN));
            floor.setHasHall(has(plan, RoomType.HALL));
            floor.setHasBalcony(has(plan, RoomType.BALCONY));
            floor.setHasPoojaRoom(has(plan, RoomType.POOJA));
            floor.setDoorCount(m.doors());
            floor.setWindowCount(m.windows());
            applied++;
        }
        if (applied == 0) {
            throw new BadRequestException("Draw and save at least one floor plan with rooms before applying it to the estimate.");
        }
        requirement.setStatus(HouseRequirementStatus.DRAFT);
        return HouseRequirementResponse.from(houseRequirementRepository.save(requirement));
    }

    // Combined drawn quantities, or null unless every floor of the requirement has a drawn plan:
    // mixing drawn floors with area-rule floors would double count or miss walls.
    PlanGeometry.Derived deriveIfFullyDrawn(HouseRequirement requirement) {
        Map<Integer, FloorPlan> saved = new HashMap<>();
        for (FloorPlan plan : floorPlanRepository.findByHouseRequirementIdOrderByFloorLevelAsc(requirement.getId())) {
            saved.put(plan.getFloorLevel(), plan);
        }
        PlanGeometry.Derived total = PlanGeometry.Derived.NONE;
        for (FloorRequirement floor : requirement.getFloors()) {
            FloorPlan plan = saved.get(floor.getFloorLevel());
            if (plan == null || plan.getRooms().isEmpty()) {
                return null;
            }
            total = total.plus(PlanGeometry.derive(toRoomDtos(plan), toOpeningDtos(plan)));
        }
        return total;
    }

    private boolean has(FloorPlan plan, RoomType type) {
        return plan.getRooms().stream().anyMatch(r -> r.getType() == type);
    }

    private PlansResponse plansFor(HouseRequirement requirement) {
        Map<Integer, FloorPlan> saved = new HashMap<>();
        for (FloorPlan plan : floorPlanRepository.findByHouseRequirementIdOrderByFloorLevelAsc(requirement.getId())) {
            saved.put(plan.getFloorLevel(), plan);
        }
        List<FloorPlanResponse> floors = new ArrayList<>();
        for (FloorRequirement floor : requirement.getFloors()) {
            FloorPlan plan = saved.get(floor.getFloorLevel());
            floors.add(plan != null
                    ? toResponse(plan)
                    : responseFor(floor.getFloorLevel(), false, List.of(), List.of(), List.of(), List.of()));
        }
        return new PlansResponse(requirement.getPlotWidthFt(), requirement.getPlotLengthFt(), floors);
    }

    private FloorPlanResponse toResponse(FloorPlan plan) {
        List<RoomDto> rooms = toRoomDtos(plan);
        List<OpeningDto> openings = toOpeningDtos(plan);
        return responseFor(plan.getFloorLevel(), true, rooms, openings, toColumnDtos(plan), toFurnitureDtos(plan));
    }

    private FloorPlanResponse responseFor(int level, boolean saved, List<RoomDto> rooms, List<OpeningDto> openings,
                                          List<ColumnDto> columns, List<FurnitureDto> furniture) {
        List<BeamDto> beams = StructureGeometry.beams(columns, rooms);
        StructureGeometry.Quantities q = StructureGeometry.quantities(level, columns, rooms);
        return new FloorPlanResponse(level, saved, rooms, openings, columns, beams, furniture, metrics(rooms, openings),
                new StructureMetrics(q.columns(), dec(q.beamFt()), dec(q.concreteM3()), dec(q.steelKg())));
    }

    private List<FurnitureDto> toFurnitureDtos(FloorPlan plan) {
        return plan.getFurniture().stream()
                .map(f -> new FurnitureDto(f.getClientId(), f.getKind(), f.getX(), f.getY(), f.getWidthFt(), f.getDepthFt(),
                        f.getHeightFt(), f.getRotationDeg(), f.getColour()))
                .toList();
    }

    private List<ColumnDto> toColumnDtos(FloorPlan plan) {
        return plan.getColumns().stream()
                .map(c -> new ColumnDto(c.getClientId(), c.getX(), c.getY(), c.getWidthFt(), c.getDepthFt()))
                .toList();
    }

    // Frame quantities for the whole building, or null unless every floor has a drawn plan with columns:
    // a partly drawn frame would understate concrete and steel, so the area rules stay in force instead.
    StructureGeometry.Quantities structureIfComplete(HouseRequirement requirement) {
        Map<Integer, FloorPlan> saved = new HashMap<>();
        for (FloorPlan plan : floorPlanRepository.findByHouseRequirementIdOrderByFloorLevelAsc(requirement.getId())) {
            saved.put(plan.getFloorLevel(), plan);
        }
        StructureGeometry.Quantities total = StructureGeometry.Quantities.NONE;
        for (FloorRequirement floor : requirement.getFloors()) {
            FloorPlan plan = saved.get(floor.getFloorLevel());
            if (plan == null || plan.getRooms().isEmpty() || plan.getColumns().size() < 4) {
                return null;
            }
            total = total.plus(StructureGeometry.quantities(floor.getFloorLevel(), toColumnDtos(plan), toRoomDtos(plan)));
        }
        return total;
    }

    private List<RoomDto> toRoomDtos(FloorPlan plan) {
        return plan.getRooms().stream()
                .map(r -> new RoomDto(r.getClientId(), r.getType(), r.getName(), r.getX(), r.getY(), r.getWidthFt(), r.getDepthFt()))
                .toList();
    }

    private List<OpeningDto> toOpeningDtos(FloorPlan plan) {
        return plan.getOpenings().stream()
                .map(o -> new OpeningDto(o.getClientId(), o.getRoomClientId(), o.getKind(), o.getSide(), o.getOffsetFt(), o.getWidthFt()))
                .toList();
    }

    private Metrics metrics(List<RoomDto> rooms, List<OpeningDto> openings) {
        if (rooms.isEmpty()) {
            BigDecimal zero = BigDecimal.ZERO.setScale(2);
            return new Metrics(zero, zero, zero, zero, 0, 0, 0, 0, zero, zero, zero, zero);
        }
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = 0;
        double maxY = 0;
        double roomsArea = 0;
        int bedrooms = 0;
        int bathrooms = 0;
        for (RoomDto room : rooms) {
            double x = room.x().doubleValue();
            double y = room.y().doubleValue();
            double w = room.widthFt().doubleValue();
            double d = room.depthFt().doubleValue();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + w);
            maxY = Math.max(maxY, y + d);
            roomsArea += w * d;
            if (room.type() == RoomType.BEDROOM) bedrooms++;
            if (room.type() == RoomType.BATHROOM) bathrooms++;
        }
        double boundingWidth = maxX - minX;
        double boundingDepth = maxY - minY;
        int doors = 0;
        int windows = 0;
        if (openings != null) {
            for (OpeningDto opening : openings) {
                if (opening.kind() == OpeningKind.DOOR) doors++;
                else windows++;
            }
        }
        // Built-up area is the sum of the drawn rooms (dimensions to wall centres), so open terrace
        // or unused footprint on a smaller upper floor is not counted.
        PlanGeometry.Derived derived = PlanGeometry.derive(rooms, openings);
        return new Metrics(dec(roomsArea), dec(roomsArea), dec(boundingWidth), dec(boundingDepth),
                bedrooms, bathrooms, doors, windows,
                dec(derived.externalWallFt()), dec(derived.internalWallFt()),
                dec(derived.externalWallAreaSqft() + derived.internalWallAreaSqft()), dec(derived.plasterAreaSqft()));
    }

    private void validate(HouseRequirement requirement, LayoutRequest layout) {
        List<RoomDto> rooms = layout.rooms();
        List<OpeningDto> openings = layout.openings() == null ? List.of() : layout.openings();
        if (rooms.size() > MAX_ROOMS) {
            throw new BadRequestException("A floor can have at most " + MAX_ROOMS + " rooms.");
        }
        if (openings.size() > MAX_OPENINGS) {
            throw new BadRequestException("A floor can have at most " + MAX_OPENINGS + " doors and windows.");
        }

        double plotWidth = requirement.getPlotWidthFt().doubleValue();
        double plotDepth = requirement.getPlotLengthFt().doubleValue();
        Map<String, RoomDto> byId = new HashMap<>();
        for (RoomDto room : rooms) {
            String label = room.name() != null && !room.name().isBlank() ? room.name() : room.id();
            if (byId.put(room.id(), room) != null) {
                throw new BadRequestException("Room id " + room.id() + " is used more than once.");
            }
            double w = room.widthFt().doubleValue();
            double d = room.depthFt().doubleValue();
            if (w < MIN_ROOM_SIDE_FT || d < MIN_ROOM_SIDE_FT) {
                throw new BadRequestException(label + " is too small; each side must be at least " + MIN_ROOM_SIDE_FT + " ft.");
            }
            if (room.x().doubleValue() + w > plotWidth + TOLERANCE_FT || room.y().doubleValue() + d > plotDepth + TOLERANCE_FT) {
                throw new BadRequestException(label + " extends beyond the plot (" + plotWidth + " x " + plotDepth + " ft).");
            }
        }

        for (int i = 0; i < rooms.size(); i++) {
            for (int j = i + 1; j < rooms.size(); j++) {
                if (overlaps(rooms.get(i), rooms.get(j))) {
                    throw new BadRequestException(nameOf(rooms.get(i)) + " overlaps " + nameOf(rooms.get(j)) + ".");
                }
            }
        }

        List<ColumnDto> columns = layout.columns() == null ? List.of() : layout.columns();
        if (columns.size() > 80) {
            throw new BadRequestException("A floor can have at most 80 columns.");
        }
        Set<String> columnIds = new HashSet<>();
        for (ColumnDto column : columns) {
            if (!columnIds.add(column.id())) {
                throw new BadRequestException("Column id " + column.id() + " is used more than once.");
            }
            if (column.x().doubleValue() > plotWidth + TOLERANCE_FT || column.y().doubleValue() > plotDepth + TOLERANCE_FT) {
                throw new BadRequestException("A column is outside the plot.");
            }
            if (column.widthFt().doubleValue() > 4 || column.depthFt().doubleValue() > 4) {
                throw new BadRequestException("A column cannot be larger than 4 ft on a side.");
            }
        }

        List<FurnitureDto> furniture = layout.furniture() == null ? List.of() : layout.furniture();
        if (furniture.size() > 80) {
            throw new BadRequestException("A floor can have at most 80 furniture items.");
        }
        Set<String> furnitureIds = new HashSet<>();
        for (FurnitureDto item : furniture) {
            if (!furnitureIds.add(item.id())) {
                throw new BadRequestException("Furniture id " + item.id() + " is used more than once.");
            }
            if (!FURNITURE_KINDS.contains(item.kind())) {
                throw new BadRequestException("Unknown furniture kind " + item.kind() + ".");
            }
            if (item.x().doubleValue() > plotWidth + TOLERANCE_FT || item.y().doubleValue() > plotDepth + TOLERANCE_FT) {
                throw new BadRequestException("A furniture item is outside the plot.");
            }
            if (item.widthFt().doubleValue() > 25 || item.depthFt().doubleValue() > 25 || item.heightFt().doubleValue() > 40) {
                throw new BadRequestException("A furniture item is too large (at most 25 x 25 ft and 40 ft high).");
            }
            if (item.rotationDeg() != 0 && item.rotationDeg() != 90 && item.rotationDeg() != 180 && item.rotationDeg() != 270) {
                throw new BadRequestException("Furniture rotation must be 0, 90, 180 or 270 degrees.");
            }
            if (item.colour() != null && !item.colour().matches("#[0-9a-fA-F]{6}")) {
                throw new BadRequestException("Furniture colour must look like #a1b2c3.");
            }
        }

        Set<String> openingIds = new HashSet<>();
        for (OpeningDto opening : openings) {
            if (!openingIds.add(opening.id())) {
                throw new BadRequestException("Opening id " + opening.id() + " is used more than once.");
            }
            RoomDto room = byId.get(opening.roomId());
            if (room == null) {
                throw new BadRequestException("A door or window refers to a room that is not on this floor.");
            }
            boolean horizontalSide = opening.side() == com.buildflow.estimation.entity.PlanSide.NORTH
                    || opening.side() == com.buildflow.estimation.entity.PlanSide.SOUTH;
            double sideLength = horizontalSide ? room.widthFt().doubleValue() : room.depthFt().doubleValue();
            if (opening.offsetFt().doubleValue() + opening.widthFt().doubleValue() > sideLength + TOLERANCE_FT) {
                throw new BadRequestException("A " + opening.kind().name().toLowerCase() + " in " + nameOf(room)
                        + " does not fit on its " + opening.side().name().toLowerCase() + " wall.");
            }
        }
    }

    // Rooms may touch (shared walls) but not overlap by more than the tolerance.
    private boolean overlaps(RoomDto a, RoomDto b) {
        double overlapX = Math.min(a.x().doubleValue() + a.widthFt().doubleValue(), b.x().doubleValue() + b.widthFt().doubleValue())
                - Math.max(a.x().doubleValue(), b.x().doubleValue());
        double overlapY = Math.min(a.y().doubleValue() + a.depthFt().doubleValue(), b.y().doubleValue() + b.depthFt().doubleValue())
                - Math.max(a.y().doubleValue(), b.y().doubleValue());
        return overlapX > TOLERANCE_FT && overlapY > TOLERANCE_FT;
    }

    private String nameOf(RoomDto room) {
        return room.name() != null && !room.name().isBlank() ? room.name() : room.id();
    }

    private HouseRequirement findOwned(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return houseRequirementRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("House requirement not found."));
    }

    private static BigDecimal dec(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
