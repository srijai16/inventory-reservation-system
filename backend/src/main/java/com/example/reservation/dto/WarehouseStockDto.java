package com.example.reservation.dto;

public class WarehouseStockDto {
    private String warehouseId;
    private String warehouseName;
    private String location;
    private Integer totalUnits;
    private Integer reservedUnits;
    private Integer availableUnits;

    public WarehouseStockDto() {}

    public WarehouseStockDto(String warehouseId, String warehouseName, String location, Integer totalUnits, Integer reservedUnits, Integer availableUnits) {
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.location = location;
        this.totalUnits = totalUnits;
        this.reservedUnits = reservedUnits;
        this.availableUnits = availableUnits;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(Integer totalUnits) {
        this.totalUnits = totalUnits;
    }

    public Integer getReservedUnits() {
        return reservedUnits;
    }

    public void setReservedUnits(Integer reservedUnits) {
        this.reservedUnits = reservedUnits;
    }

    public Integer getAvailableUnits() {
        return availableUnits;
    }

    public void setAvailableUnits(Integer availableUnits) {
        this.availableUnits = availableUnits;
    }
}
