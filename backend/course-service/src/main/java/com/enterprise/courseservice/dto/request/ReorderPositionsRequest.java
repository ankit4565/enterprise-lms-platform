package com.enterprise.courseservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderPositionsRequest {

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<ItemPosition> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemPosition {
        @NotNull(message = "Item ID is required")
        private UUID id;

        @NotNull(message = "Position is required")
        private Integer position;
    }
}
