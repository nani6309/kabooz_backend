package com.kabooz.backend.dto.response;

import com.kabooz.backend.entity.Distributor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO representing a full distributor row.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributorResponse {

    private Long          id;
    private String        name;
    private String        mobile;
    private String        shopName;
    private String        address;
    private String        status;
    private LocalDateTime createdAt;

    /**
     * Map a {@link Distributor} entity to its response DTO.
     *
     * @param d the entity (non-null)
     * @return mapped DTO
     */
    public static DistributorResponse from(Distributor d) {
        return DistributorResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .mobile(d.getMobile())
                .shopName(d.getShopName())
                .address(d.getAddress())
                .status(d.getStatus() != null ? d.getStatus().name() : Distributor.Status.NEW.name())
                .createdAt(d.getCreatedAt())
                .build();
    }
}

