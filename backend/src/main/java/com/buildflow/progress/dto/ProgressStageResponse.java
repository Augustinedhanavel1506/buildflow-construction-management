package com.buildflow.progress.dto;

import com.buildflow.progress.entity.ProjectProgressStage;

public record ProgressStageResponse(
        Long id,
        Long projectId,
        String stageName,
        int percentComplete,
        int sortOrder
) {
    public static ProgressStageResponse from(ProjectProgressStage stage) {
        return new ProgressStageResponse(
                stage.getId(),
                stage.getProject().getId(),
                stage.getStageName(),
                stage.getPercentComplete(),
                stage.getSortOrder()
        );
    }
}
