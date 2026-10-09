package com.buildflow.dealer.service;

import com.buildflow.business.entity.Business;
import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.dealer.dto.DealerRateRequest;
import com.buildflow.dealer.dto.DealerRequest;
import com.buildflow.dealer.dto.DealerResponse;
import com.buildflow.dealer.entity.Dealer;
import com.buildflow.dealer.entity.DealerRate;
import com.buildflow.dealer.repository.DealerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DealerService {

    private final DealerRepository dealerRepository;
    private final CurrentUserProvider currentUserProvider;

    public DealerService(DealerRepository dealerRepository, CurrentUserProvider currentUserProvider) {
        this.dealerRepository = dealerRepository;
        this.currentUserProvider = currentUserProvider;
    }

    // district and item are optional case-insensitive filters ("dealers in Madurai who sell cement").
    @Transactional(readOnly = true)
    public List<DealerResponse> list(String district, String item, boolean includeInactive) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return dealerRepository.findByBusinessIdOrderByNameAsc(businessId).stream()
                .filter(d -> includeInactive || d.isActive())
                .filter(d -> district == null || district.isBlank()
                        || (d.getDistrict() != null && d.getDistrict().equalsIgnoreCase(district.trim())))
                .filter(d -> item == null || item.isBlank() || d.getRates().stream()
                        .anyMatch(r -> r.getItemName().toLowerCase(Locale.ROOT).contains(item.trim().toLowerCase(Locale.ROOT))))
                .map(DealerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DealerResponse get(Long id) {
        return DealerResponse.from(findOwned(id));
    }

    @Transactional
    public DealerResponse create(DealerRequest request) {
        Business business = currentUserProvider.getCurrentUser().getBusiness();
        Dealer dealer = new Dealer();
        dealer.setBusiness(business);
        applyRequest(dealer, request);
        return DealerResponse.from(dealerRepository.save(dealer));
    }

    @Transactional
    public DealerResponse update(Long id, DealerRequest request) {
        Dealer dealer = findOwned(id);
        applyRequest(dealer, request);
        return DealerResponse.from(dealerRepository.save(dealer));
    }

    private void applyRequest(Dealer dealer, DealerRequest request) {
        dealer.setName(request.name().trim());
        dealer.setContactPerson(request.contactPerson());
        dealer.setPhone(request.phone());
        dealer.setEmail(request.email());
        dealer.setAddress(request.address());
        dealer.setDistrict(request.district());
        dealer.setDeliveryRadiusKm(request.deliveryRadiusKm());
        dealer.setGstin(request.gstin());
        dealer.setNotes(request.notes());
        dealer.setActive(request.active() == null || request.active());

        if (request.rates() != null) {
            Set<String> seen = new HashSet<>();
            for (DealerRateRequest rate : request.rates()) {
                if (!seen.add(rate.itemName().trim().toLowerCase(Locale.ROOT))) {
                    throw new BadRequestException("Duplicate rate for item: " + rate.itemName().trim());
                }
            }
            // Same collection instance, so orphanRemoval drops rates that were removed.
            dealer.getRates().clear();
            for (DealerRateRequest rate : request.rates()) {
                DealerRate line = new DealerRate();
                line.setDealer(dealer);
                line.setItemName(rate.itemName().trim());
                line.setUnit(rate.unit().trim());
                line.setRate(rate.rate());
                dealer.getRates().add(line);
            }
        }
    }

    private Dealer findOwned(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return dealerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found."));
    }
}
