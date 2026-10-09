package com.buildflow.calculator.service;

import com.buildflow.calculator.dto.CalculatorModels.Bar;
import com.buildflow.calculator.dto.CalculatorModels.BarResult;
import com.buildflow.calculator.dto.CalculatorModels.ConcreteMember;
import com.buildflow.calculator.dto.CalculatorModels.ConcreteRequest;
import com.buildflow.calculator.dto.CalculatorModels.ConcreteResult;
import com.buildflow.calculator.dto.CalculatorModels.DiameterTotal;
import com.buildflow.calculator.dto.CalculatorModels.MasonryRequest;
import com.buildflow.calculator.dto.CalculatorModels.MasonryResult;
import com.buildflow.calculator.dto.CalculatorModels.MemberResult;
import com.buildflow.calculator.dto.CalculatorModels.MemberType;
import com.buildflow.calculator.dto.CalculatorModels.MixGrade;
import com.buildflow.calculator.dto.CalculatorModels.SteelRequest;
import com.buildflow.calculator.dto.CalculatorModels.SteelResult;
import com.buildflow.calculator.dto.CalculatorModels.UnitType;
import com.buildflow.calculator.dto.CalculatorModels.Wall;
import com.buildflow.calculator.dto.CalculatorModels.WallResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Stateless working formulas an engineer would otherwise do by hand. Every result lists the
 * assumptions used, so the numbers can be checked and a different assumption can be argued.
 * Nominal-mix ratios follow IS 456; these are estimating quantities, not a structural design.
 */
@Service
public class CalculatorService {

    private static final double STEEL_DENSITY_KG_M3 = 7850;
    private static final double DRY_VOLUME_FACTOR_CONCRETE = 1.54;
    private static final double DRY_VOLUME_FACTOR_MORTAR = 1.33;
    // One 50 kg bag of cement occupies about 0.0347 m3.
    private static final double CEMENT_BAG_M3 = 0.0347;

    public ConcreteResult concrete(ConcreteRequest request) {
        double[] ratio = ratio(request.grade());
        double ratioSum = ratio[0] + ratio[1] + ratio[2];
        double wastage = pct(request.wastagePercent());

        List<MemberResult> members = new ArrayList<>();
        double totalVolume = 0;
        double totalSteel = 0;
        for (ConcreteMember m : request.members()) {
            double volume = m.count() * m.lengthM().doubleValue() * m.widthM().doubleValue() * m.depthM().doubleValue();
            double steelPct = m.steelPercent() != null ? m.steelPercent().doubleValue() : defaultSteelPercent(m.type());
            double steelKg = volume * steelPct / 100 * STEEL_DENSITY_KG_M3;
            members.add(new MemberResult(m.name(), m.type().name(), m.count(), r3(volume), r2(steelPct), r2(steelKg)));
            totalVolume += volume;
            totalSteel += steelKg;
        }

        double dryVolume = totalVolume * DRY_VOLUME_FACTOR_CONCRETE;
        double factor = 1 + wastage / 100;
        double cementM3 = dryVolume * ratio[0] / ratioSum * factor;
        double sandM3 = dryVolume * ratio[1] / ratioSum * factor;
        double aggregateM3 = dryVolume * ratio[2] / ratioSum * factor;

        return new ConcreteResult(
                request.grade().name(), "1:" + trim(ratio[1]) + ":" + trim(ratio[2]), members,
                r3(totalVolume), r2(cementM3 / CEMENT_BAG_M3), r3(sandM3), r3(aggregateM3),
                r2(totalSteel * factor), r2(wastage),
                List.of(
                        "Nominal mix " + request.grade().name() + " = 1:" + trim(ratio[1]) + ":" + trim(ratio[2]) + " (cement:sand:aggregate).",
                        "Dry volume = " + DRY_VOLUME_FACTOR_CONCRETE + " x wet volume.",
                        "1 bag of cement (50 kg) = 0.0347 m3.",
                        "Steel = member volume x steel % x 7850 kg/m3. Default steel %: footing 0.8, column 1.5, beam 1.5, slab 0.8, other 1.0; override per member.",
                        "Wastage of " + trim(wastage) + "% is applied to cement, sand, aggregate and steel."));
    }

    public MasonryResult masonry(MasonryRequest request) {
        UnitSpec spec = spec(request.unitType());
        int sandParts = request.mortarSandParts() != null ? request.mortarSandParts() : 6;
        double wastage = pct(request.wastagePercent());

        List<WallResult> walls = new ArrayList<>();
        double totalUnits = 0;
        double totalVolume = 0;
        for (Wall w : request.walls()) {
            double gross = w.lengthM().doubleValue() * w.heightM().doubleValue();
            double openings = w.openingsSqm() != null ? w.openingsSqm().doubleValue() : 0;
            double net = Math.max(gross - openings, 0);
            double volume = net * w.thicknessM().doubleValue();
            double units = volume / spec.nominalVolume();
            walls.add(new WallResult(w.name(), r2(gross), r2(net), r3(volume), r2(units)));
            totalUnits += units;
            totalVolume += volume;
        }

        // Mortar fills whatever the units do not: wall volume minus the volume of the units alone.
        double mortarWet = Math.max(totalVolume - totalUnits * spec.actualVolume(), 0);
        double mortarDry = mortarWet * DRY_VOLUME_FACTOR_MORTAR;
        double factor = 1 + wastage / 100;
        double cementM3 = mortarDry / (1 + sandParts) * factor;
        double sandM3 = mortarDry * sandParts / (1 + sandParts) * factor;

        return new MasonryResult(
                request.unitType().name(), sandParts, walls,
                r2(totalUnits * factor), r3(mortarDry * factor), r2(cementM3 / CEMENT_BAG_M3), r3(sandM3), r2(wastage),
                List.of(
                        spec.description(),
                        "Units are counted from net wall volume (openings deducted) divided by the nominal unit volume including mortar joints.",
                        "Mortar = wall volume minus the volume of the units themselves; dry volume = " + DRY_VOLUME_FACTOR_MORTAR + " x wet.",
                        "Mortar mix 1:" + sandParts + " (cement:sand).",
                        "Wastage of " + trim(wastage) + "% is applied to units, cement and sand."));
    }

    public SteelResult steel(SteelRequest request) {
        double wastage = pct(request.wastagePercent());
        double standardLength = request.standardBarLengthM() != null ? request.standardBarLengthM().doubleValue() : 12;

        List<BarResult> bars = new ArrayList<>();
        Map<Integer, double[]> byDiameter = new TreeMap<>();
        double totalKg = 0;
        for (Bar bar : request.bars()) {
            double totalLength = bar.lengthM().doubleValue() * bar.nos();
            double weight = unitWeightKgPerM(bar.diameterMm()) * totalLength;
            bars.add(new BarResult(bar.mark(), bar.diameterMm(), r2(totalLength), r2(weight)));
            double[] sums = byDiameter.computeIfAbsent(bar.diameterMm(), d -> new double[2]);
            sums[0] += totalLength;
            sums[1] += weight;
            totalKg += weight;
        }

        List<DiameterTotal> totals = new ArrayList<>();
        for (Map.Entry<Integer, double[]> entry : byDiameter.entrySet()) {
            double[] sums = entry.getValue();
            totals.add(new DiameterTotal(entry.getKey(), r2(sums[0]), r2(sums[1]), (int) Math.ceil(sums[0] / standardLength)));
        }

        return new SteelResult(bars, totals, r2(totalKg), r2(totalKg * (1 + wastage / 100)), r2(wastage),
                List.of(
                        "Unit weight = d^2 / 162.2 kg per metre, with d in millimetres.",
                        "Lengths entered are cut lengths; add laps, hooks and bends to them before entering.",
                        "Standard bars required = total length / " + trim(standardLength) + " m, rounded up, ignoring lap and cutting waste.",
                        "Wastage of " + trim(wastage) + "% is applied to the total weight."));
    }

    private static double unitWeightKgPerM(int diameterMm) {
        return (double) diameterMm * diameterMm / 162.2;
    }

    private static double defaultSteelPercent(MemberType type) {
        return switch (type) {
            case FOOTING, SLAB -> 0.8;
            case COLUMN, BEAM -> 1.5;
            case OTHER -> 1.0;
        };
    }

    // Parts of cement : sand : aggregate for the nominal mixes in IS 456.
    private static double[] ratio(MixGrade grade) {
        return switch (grade) {
            case M15 -> new double[]{1, 2, 4};
            case M20 -> new double[]{1, 1.5, 3};
            case M25 -> new double[]{1, 1, 2};
        };
    }

    private record UnitSpec(double nominalL, double nominalH, double nominalT,
                            double actualL, double actualH, double actualT, String description) {
        double nominalVolume() {
            return nominalL * nominalH * nominalT;
        }

        double actualVolume() {
            return actualL * actualH * actualT;
        }
    }

    private static UnitSpec spec(UnitType type) {
        return switch (type) {
            case RED_BRICK -> new UnitSpec(0.20, 0.10, 0.10, 0.19, 0.09, 0.09,
                    "Red brick: 190 x 90 x 90 mm actual, 200 x 100 x 100 mm nominal with 10 mm joints (500 per m3).");
            case AAC_BLOCK -> new UnitSpec(0.61, 0.21, 0.20, 0.60, 0.20, 0.20,
                    "AAC block: 600 x 200 x 200 mm actual, 610 x 210 x 200 mm nominal with 10 mm joints.");
            case SOLID_BLOCK -> new UnitSpec(0.41, 0.21, 0.20, 0.40, 0.20, 0.20,
                    "Solid concrete block: 400 x 200 x 200 mm actual, 410 x 210 x 200 mm nominal with 10 mm joints.");
        };
    }

    private static double pct(BigDecimal value) {
        return value != null ? value.doubleValue() : 0;
    }

    private static BigDecimal r2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal r3(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP);
    }

    private static String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
