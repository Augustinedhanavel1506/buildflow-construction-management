package com.buildflow.ratemaster.service;

import com.buildflow.boq.entity.BoqCategory;

import java.math.BigDecimal;
import java.util.List;

/**
 * Illustrative starting rates for every item the estimation rules use, so a new business can get a
 * believable estimate without typing eight to twenty rates first. They are rough Tamil Nadu retail
 * figures, not live prices; each is stored with a ±8% band and a note telling the user to replace it.
 */
final class StarterRates {

    record Rate(String itemName, BoqCategory category, String unit, String rate) {
    }

    static final List<Rate> ALL = List.of(
            new Rate("OPC 53 Grade Cement", BoqCategory.MATERIAL, "bag", "420"),
            new Rate("TMT Steel Fe500", BoqCategory.MATERIAL, "kg", "65"),
            new Rate("Red Clay Brick 9x4x3", BoqCategory.MATERIAL, "nos", "9"),
            new Rate("River Sand", BoqCategory.MATERIAL, "cum", "1800"),
            new Rate("Aggregate 20mm", BoqCategory.MATERIAL, "cum", "1600"),
            new Rate("Vitrified Tile", BoqCategory.MATERIAL, "sqft", "65"),
            new Rate("Emulsion Paint", BoqCategory.MATERIAL, "ltr", "220"),
            new Rate("Flush Door", BoqCategory.MATERIAL, "nos", "3500"),
            new Rate("Waterproofing Chemical", BoqCategory.MATERIAL, "kg", "180"),
            new Rate("Copper Wire", BoqCategory.MATERIAL, "m", "22"),
            new Rate("Electrical Conduit Pipe", BoqCategory.MATERIAL, "m", "18"),
            new Rate("Switch and Socket", BoqCategory.MATERIAL, "nos", "120"),
            new Rate("Water Supply Pipe", BoqCategory.MATERIAL, "m", "45"),
            new Rate("Sanitary Fittings Set", BoqCategory.MATERIAL, "nos", "12000"),
            new Rate("Mason Labour", BoqCategory.LABOUR, "day", "900"),
            new Rate("Helper Labour", BoqCategory.LABOUR, "day", "600"),
            new Rate("Bar Bender Labour", BoqCategory.LABOUR, "day", "800"),
            new Rate("Electrician Labour", BoqCategory.LABOUR, "day", "900"),
            new Rate("Plumber Labour", BoqCategory.LABOUR, "day", "900"),
            new Rate("Painter Labour", BoqCategory.LABOUR, "day", "800"));

    static final BigDecimal BAND = new BigDecimal("0.08");

    private StarterRates() {
    }
}
