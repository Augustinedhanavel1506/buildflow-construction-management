package com.buildflow.ratemaster.service;

import com.buildflow.business.entity.Business;
import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.ratemaster.dto.RateMasterItemRequest;
import com.buildflow.ratemaster.dto.RateMasterItemResponse;
import com.buildflow.ratemaster.entity.RateMasterItem;
import com.buildflow.ratemaster.repository.RateMasterItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RateMasterItemService {

    private final RateMasterItemRepository rateMasterItemRepository;
    private final CurrentUserProvider currentUserProvider;

    public RateMasterItemService(RateMasterItemRepository rateMasterItemRepository,
                                  CurrentUserProvider currentUserProvider) {
        this.rateMasterItemRepository = rateMasterItemRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<RateMasterItemResponse> list(boolean includeInactive) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        List<RateMasterItem> items = includeInactive
                ? rateMasterItemRepository.findByBusinessIdOrderByItemNameAsc(businessId)
                : rateMasterItemRepository.findByBusinessIdAndActiveTrueOrderByItemNameAsc(businessId);
        return items.stream().map(RateMasterItemResponse::from).toList();
    }

    @Transactional
    public RateMasterItemResponse create(RateMasterItemRequest request) {
        Business business = currentUserProvider.getCurrentUser().getBusiness();
        rejectDuplicate(business.getId(), null, request);

        RateMasterItem item = new RateMasterItem();
        item.setBusiness(business);
        applyRequest(item, request);

        return RateMasterItemResponse.from(rateMasterItemRepository.save(item));
    }

    @Transactional
    public RateMasterItemResponse update(Long id, RateMasterItemRequest request) {
        RateMasterItem item = findOwnedItem(id);
        rejectDuplicate(item.getBusiness().getId(), item.getId(), request);
        applyRequest(item, request);
        return RateMasterItemResponse.from(rateMasterItemRepository.save(item));
    }

    // Adds the illustrative starter rate for every estimation item the business does not already
    // have (matched by name, ignoring case). Existing rates, active or not, are never touched.
    @Transactional
    public StarterRatesResult addStarterRates() {
        Business business = currentUserProvider.getCurrentUser().getBusiness();
        java.util.Set<String> existing = new java.util.HashSet<>();
        for (RateMasterItem item : rateMasterItemRepository.findByBusinessIdOrderByItemNameAsc(business.getId())) {
            if (item.getDistrict() == null) {
                existing.add(item.getItemName().trim().toLowerCase());
            }
        }

        List<RateMasterItem> created = new java.util.ArrayList<>();
        for (StarterRates.Rate starter : StarterRates.ALL) {
            if (existing.contains(starter.itemName().toLowerCase())) {
                continue;
            }
            java.math.BigDecimal rate = new java.math.BigDecimal(starter.rate());
            RateMasterItem item = new RateMasterItem();
            item.setBusiness(business);
            item.setItemName(starter.itemName());
            item.setCategory(starter.category());
            item.setUnit(starter.unit());
            item.setStandardRate(rate);
            item.setMinRate(rate.multiply(java.math.BigDecimal.ONE.subtract(StarterRates.BAND)).setScale(2, java.math.RoundingMode.HALF_UP));
            item.setMaxRate(rate.multiply(java.math.BigDecimal.ONE.add(StarterRates.BAND)).setScale(2, java.math.RoundingMode.HALF_UP));
            item.setNotes("Starter rate (illustrative). Replace with your local rate.");
            created.add(item);
        }
        rateMasterItemRepository.saveAll(created);
        return new StarterRatesResult(created.size(), StarterRates.ALL.size() - created.size());
    }

    public record StarterRatesResult(int added, int alreadyPresent) {
    }

    static String normalizeDistrict(String district) {
        return district == null || district.isBlank() ? null : district.trim();
    }

    private static boolean sameDistrict(String a, String b) {
        return a == null ? b == null : b != null && a.equalsIgnoreCase(b);
    }

    // One rate per item and district: two rates for the same item in the same place would make the
    // price an estimate uses depend on which one happened to be read first.
    private void rejectDuplicate(Long businessId, Long ignoreId, RateMasterItemRequest request) {
        String district = normalizeDistrict(request.district());
        for (RateMasterItem other : rateMasterItemRepository.findByBusinessIdOrderByItemNameAsc(businessId)) {
            if (other.getId().equals(ignoreId)) {
                continue;
            }
            if (other.getItemName().trim().equalsIgnoreCase(request.itemName().trim()) && sameDistrict(other.getDistrict(), district)) {
                throw new BadRequestException(district == null
                        ? "A default rate for " + request.itemName().trim() + " already exists."
                        : "A rate for " + request.itemName().trim() + " in " + district + " already exists.");
            }
        }
    }

    public record CopyDistrictResult(int added, int skipped) {
    }

    /**
     * Copies one district's rates (or the defaults, when fromDistrict is blank) into another district,
     * scaled by a percentage, for items the target district does not have yet. No regional prices are
     * invented: the user supplies the adjustment and then edits individual rates.
     */
    @Transactional
    public CopyDistrictResult copyToDistrict(String fromDistrict, String toDistrict, java.math.BigDecimal adjustPercent) {
        String from = normalizeDistrict(fromDistrict);
        String to = normalizeDistrict(toDistrict);
        if (to == null) {
            throw new BadRequestException("Choose the district to copy the rates into.");
        }
        if (sameDistrict(from, to)) {
            throw new BadRequestException("Choose a different district to copy into.");
        }
        java.math.BigDecimal percent = adjustPercent == null ? java.math.BigDecimal.ZERO : adjustPercent;
        if (percent.compareTo(new java.math.BigDecimal("-50")) < 0 || percent.compareTo(new java.math.BigDecimal("100")) > 0) {
            throw new BadRequestException("The adjustment must be between -50% and +100%.");
        }

        Business business = currentUserProvider.getCurrentUser().getBusiness();
        List<RateMasterItem> all = rateMasterItemRepository.findByBusinessIdOrderByItemNameAsc(business.getId());
        java.util.Set<String> existingInTarget = new java.util.HashSet<>();
        for (RateMasterItem item : all) {
            if (sameDistrict(item.getDistrict(), to)) {
                existingInTarget.add(item.getItemName().trim().toLowerCase());
            }
        }

        java.math.BigDecimal factor = java.math.BigDecimal.ONE.add(percent.divide(java.math.BigDecimal.valueOf(100)));
        List<RateMasterItem> created = new java.util.ArrayList<>();
        int skipped = 0;
        for (RateMasterItem source : all) {
            if (!source.isActive() || !sameDistrict(source.getDistrict(), from)) {
                continue;
            }
            if (existingInTarget.contains(source.getItemName().trim().toLowerCase())) {
                skipped++;
                continue;
            }
            RateMasterItem copy = new RateMasterItem();
            copy.setBusiness(business);
            copy.setItemName(source.getItemName());
            copy.setCategory(source.getCategory());
            copy.setUnit(source.getUnit());
            copy.setStandardRate(scaled(source.getStandardRate(), factor));
            copy.setMinRate(source.getMinRate() == null ? null : scaled(source.getMinRate(), factor));
            copy.setMaxRate(source.getMaxRate() == null ? null : scaled(source.getMaxRate(), factor));
            copy.setConsumptionPerSqft(source.getConsumptionPerSqft());
            copy.setDistrict(to);
            copy.setNotes("Copied from " + (from == null ? "default rates" : from)
                    + (percent.signum() == 0 ? "" : " adjusted " + (percent.signum() > 0 ? "+" : "") + percent.stripTrailingZeros().toPlainString() + "%")
                    + ". Replace with the local rate.");
            created.add(copy);
        }
        rateMasterItemRepository.saveAll(created);
        return new CopyDistrictResult(created.size(), skipped);
    }

    private static java.math.BigDecimal scaled(java.math.BigDecimal value, java.math.BigDecimal factor) {
        return value.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void applyRequest(RateMasterItem item, RateMasterItemRequest request) {
        if (request.minRate() != null && request.maxRate() != null
                && request.minRate().compareTo(request.maxRate()) > 0) {
            throw new BadRequestException("Minimum rate cannot be greater than maximum rate.");
        }

        item.setItemName(request.itemName());
        item.setCategory(request.category());
        item.setUnit(request.unit());
        item.setStandardRate(request.standardRate());
        item.setMinRate(request.minRate());
        item.setMaxRate(request.maxRate());
        item.setConsumptionPerSqft(request.consumptionPerSqft());
        item.setNotes(request.notes());
        item.setDistrict(normalizeDistrict(request.district()));
        item.setActive(request.active() == null || request.active());
    }

    private RateMasterItem findOwnedItem(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return rateMasterItemRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Rate master item not found."));
    }
}
