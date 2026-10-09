package com.backend.orbitflow.domain.payment.dto.response;

import com.backend.orbitflow.domain.payment.enums.PointPackage;

public record PointPackageResponse(
        PointPackage pointPackage,
        int amount,
        int point
) {

    public static PointPackageResponse from(PointPackage pointPackage) {
        return new PointPackageResponse(pointPackage, pointPackage.getAmount(), pointPackage.getPoint());
    }
}
