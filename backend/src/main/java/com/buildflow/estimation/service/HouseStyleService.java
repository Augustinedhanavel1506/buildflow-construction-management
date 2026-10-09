package com.buildflow.estimation.service;

import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.estimation.entity.HouseRequirement;
import com.buildflow.estimation.repository.HouseRequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Stores the 3D view's colour choices (walls, roof, room paint) with the house so they follow the user across devices. */
@Service
public class HouseStyleService {

    private static final int MAX_CHARS = 8000;

    private final HouseRequirementRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public HouseStyleService(HouseRequirementRepository repository, CurrentUserProvider currentUserProvider, ObjectMapper objectMapper) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public JsonNode get(Long id) {
        String json = findOwned(id).getHouseStyle();
        return json == null ? objectMapper.createObjectNode() : objectMapper.readTree(json);
    }

    @Transactional
    public JsonNode save(Long id, JsonNode style) {
        if (style == null || !style.isObject()) {
            throw new BadRequestException("House style must be a JSON object.");
        }
        String json = style.toString();
        if (json.length() > MAX_CHARS) {
            throw new BadRequestException("House style is too large.");
        }
        findOwned(id).setHouseStyle(json);
        return style;
    }

    private HouseRequirement findOwned(Long id) {
        return repository.findByIdAndBusinessId(id, currentUserProvider.getCurrentBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("House requirement not found."));
    }
}
