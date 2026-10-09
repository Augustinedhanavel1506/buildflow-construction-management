package com.buildflow.estimation.service;

import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.estimation.dto.WhatIfModels.ComponentDiff;
import com.buildflow.estimation.dto.WhatIfModels.FloorChange;
import com.buildflow.estimation.dto.WhatIfModels.ItemDiff;
import com.buildflow.estimation.dto.WhatIfModels.NewFloor;
import com.buildflow.estimation.dto.WhatIfModels.Summary;
import com.buildflow.estimation.dto.WhatIfModels.WhatIfRequest;
import com.buildflow.estimation.dto.WhatIfModels.WhatIfResponse;
import com.buildflow.estimation.entity.FloorRequirement;
import com.buildflow.estimation.entity.HouseRequirement;
import com.buildflow.estimation.repository.HouseRequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Answers "what if I change this?" by pricing a temporary copy of the plan with the changes applied
 * and comparing it with the plan as it stands. Nothing is saved. Both sides use the built-up-area
 * rules, so the difference reflects only the change and not a switch between estimating methods.
 */
@Service
public class WhatIfService {

    private final HouseRequirementRepository houseRequirementRepository;
    private final BoqGenerationService generationService;
    private final CurrentUserProvider currentUserProvider;

    public WhatIfService(HouseRequirementRepository houseRequirementRepository,
                          BoqGenerationService generationService,
                          CurrentUserProvider currentUserProvider) {
        this.houseRequirementRepository = houseRequirementRepository;
        this.generationService = generationService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public WhatIfResponse compare(Long requirementId, WhatIfRequest request) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        HouseRequirement base = houseRequirementRepository.findByIdAndBusinessId(requirementId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("House requirement not found."));

        BoqGenerationService.ComparisonEstimate baseline = generationService.comparisonEstimate(copy(base, null));
        BoqGenerationService.ComparisonEstimate scenario = generationService.comparisonEstimate(copy(base, request));

        BigDecimal difference = scenario.total().subtract(baseline.total());
        BigDecimal percent = baseline.total().signum() == 0 ? null
                : difference.multiply(BigDecimal.valueOf(100)).divide(baseline.total(), 1, RoundingMode.HALF_UP);

        Set<String> unpriced = new TreeSet<>(baseline.unpricedItems());
        unpriced.addAll(scenario.unpricedItems());

        return new WhatIfResponse(
                new Summary(baseline.total(), baseline.builtUpAreaSqft()),
                new Summary(scenario.total(), scenario.builtUpAreaSqft()),
                difference, percent,
                componentDiffs(baseline, scenario),
                itemDiffs(baseline, scenario),
                new ArrayList<>(unpriced));
    }

    // A transient copy of the requirement with the requested changes; it is never persisted.
    private HouseRequirement copy(HouseRequirement source, WhatIfRequest changes) {
        HouseRequirement copy = new HouseRequirement();
        copy.setBusiness(source.getBusiness());
        copy.setProject(source.getProject());
        copy.setLabel(source.getLabel());
        copy.setLocation(source.getLocation());
        copy.setPlotWidthFt(source.getPlotWidthFt());
        copy.setPlotLengthFt(source.getPlotLengthFt());
        copy.setConstructionGrade(changes != null && changes.constructionGrade() != null
                ? changes.constructionGrade() : source.getConstructionGrade());
        copy.setStructureType(source.getStructureType());
        copy.setWallMaterial(source.getWallMaterial());
        copy.setRoofType(source.getRoofType());

        Map<Integer, FloorChange> overrides = new LinkedHashMap<>();
        Set<Integer> removed = new HashSet<>();
        if (changes != null) {
            Set<Integer> levels = new HashSet<>();
            source.getFloors().forEach(f -> levels.add(f.getFloorLevel()));
            if (changes.floors() != null) {
                for (FloorChange change : changes.floors()) {
                    if (!levels.contains(change.floorLevel())) {
                        throw new BadRequestException("This plan has no floor " + change.floorLevel() + ".");
                    }
                    overrides.put(change.floorLevel(), change);
                }
            }
            if (changes.removeFloorLevels() != null) {
                for (Integer level : changes.removeFloorLevels()) {
                    if (!levels.contains(level)) {
                        throw new BadRequestException("This plan has no floor " + level + " to remove.");
                    }
                    removed.add(level);
                }
            }
        }

        int highest = -1;
        for (FloorRequirement floor : source.getFloors()) {
            highest = Math.max(highest, floor.getFloorLevel());
            if (removed.contains(floor.getFloorLevel())) {
                continue;
            }
            FloorRequirement c = new FloorRequirement();
            c.setHouseRequirement(copy);
            c.setFloorLevel(floor.getFloorLevel());
            c.setFloorAreaSqft(floor.getFloorAreaSqft());
            c.setBedroomCount(floor.getBedroomCount());
            c.setBathroomCount(floor.getBathroomCount());
            c.setHasKitchen(floor.isHasKitchen());
            c.setHasHall(floor.isHasHall());
            c.setHasBalcony(floor.isHasBalcony());
            c.setHasPoojaRoom(floor.isHasPoojaRoom());
            c.setDoorCount(floor.getDoorCount());
            c.setWindowCount(floor.getWindowCount());

            FloorChange o = overrides.get(floor.getFloorLevel());
            if (o != null) {
                if (o.floorAreaSqft() != null) c.setFloorAreaSqft(o.floorAreaSqft());
                if (o.bedroomCount() != null) c.setBedroomCount(o.bedroomCount());
                if (o.bathroomCount() != null) c.setBathroomCount(o.bathroomCount());
                if (o.hasKitchen() != null) c.setHasKitchen(o.hasKitchen());
                if (o.hasHall() != null) c.setHasHall(o.hasHall());
                if (o.hasBalcony() != null) c.setHasBalcony(o.hasBalcony());
                if (o.hasPoojaRoom() != null) c.setHasPoojaRoom(o.hasPoojaRoom());
                if (o.doorCount() != null) c.setDoorCount(o.doorCount());
                if (o.windowCount() != null) c.setWindowCount(o.windowCount());
            }
            copy.getFloors().add(c);
        }

        if (changes != null && changes.addFloors() != null) {
            int level = highest;
            for (NewFloor added : changes.addFloors()) {
                level++;
                FloorRequirement c = new FloorRequirement();
                c.setHouseRequirement(copy);
                c.setFloorLevel(level);
                c.setFloorAreaSqft(added.floorAreaSqft());
                c.setBedroomCount(added.bedroomCount() != null ? added.bedroomCount() : 0);
                c.setBathroomCount(added.bathroomCount() != null ? added.bathroomCount() : 0);
                c.setHasKitchen(Boolean.TRUE.equals(added.hasKitchen()));
                c.setHasHall(Boolean.TRUE.equals(added.hasHall()));
                c.setHasBalcony(Boolean.TRUE.equals(added.hasBalcony()));
                c.setHasPoojaRoom(Boolean.TRUE.equals(added.hasPoojaRoom()));
                c.setDoorCount(added.doorCount() != null ? added.doorCount() : 0);
                c.setWindowCount(added.windowCount() != null ? added.windowCount() : 0);
                copy.getFloors().add(c);
            }
        }

        if (copy.getFloors().isEmpty()) {
            throw new BadRequestException("A scenario needs at least one floor.");
        }
        return copy;
    }

    private List<ComponentDiff> componentDiffs(BoqGenerationService.ComparisonEstimate baseline,
                                                BoqGenerationService.ComparisonEstimate scenario) {
        Map<String, BigDecimal[]> byComponent = new LinkedHashMap<>();
        for (BoqGenerationService.ComparisonLine line : baseline.lines()) {
            BigDecimal[] sums = byComponent.computeIfAbsent(line.component(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            sums[0] = sums[0].add(line.amount());
        }
        for (BoqGenerationService.ComparisonLine line : scenario.lines()) {
            BigDecimal[] sums = byComponent.computeIfAbsent(line.component(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            sums[1] = sums[1].add(line.amount());
        }
        List<ComponentDiff> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> entry : byComponent.entrySet()) {
            BigDecimal[] sums = entry.getValue();
            result.add(new ComponentDiff(entry.getKey(), sums[0], sums[1], sums[1].subtract(sums[0])));
        }
        result.sort(Comparator.comparing((ComponentDiff d) -> d.difference().abs()).reversed());
        return result;
    }

    // Only lines whose cost changed, largest change first.
    private List<ItemDiff> itemDiffs(BoqGenerationService.ComparisonEstimate baseline,
                                      BoqGenerationService.ComparisonEstimate scenario) {
        Map<String, BigDecimal[]> byItem = new LinkedHashMap<>();
        Map<String, String[]> names = new LinkedHashMap<>();
        for (BoqGenerationService.ComparisonLine line : baseline.lines()) {
            String key = line.itemName() + "|" + line.unit();
            names.put(key, new String[]{line.itemName(), line.unit()});
            BigDecimal[] sums = byItem.computeIfAbsent(key, k -> zeros());
            sums[0] = sums[0].add(line.quantity());
            sums[2] = sums[2].add(line.amount());
        }
        for (BoqGenerationService.ComparisonLine line : scenario.lines()) {
            String key = line.itemName() + "|" + line.unit();
            names.put(key, new String[]{line.itemName(), line.unit()});
            BigDecimal[] sums = byItem.computeIfAbsent(key, k -> zeros());
            sums[1] = sums[1].add(line.quantity());
            sums[3] = sums[3].add(line.amount());
        }
        List<ItemDiff> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> entry : byItem.entrySet()) {
            BigDecimal[] s = entry.getValue();
            BigDecimal difference = s[3].subtract(s[2]);
            if (difference.signum() != 0) {
                String[] n = names.get(entry.getKey());
                result.add(new ItemDiff(n[0], n[1], s[0], s[1], s[2], s[3], difference));
            }
        }
        result.sort(Comparator.comparing((ItemDiff d) -> d.difference().abs()).reversed());
        return result;
    }

    private static BigDecimal[] zeros() {
        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
    }
}
