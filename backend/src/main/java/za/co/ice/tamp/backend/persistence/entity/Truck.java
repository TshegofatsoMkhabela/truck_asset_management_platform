package za.co.ice.tamp.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Maps {@code trucks} (db/migrations/V02__trucks.sql, capacity_m3 added in V11). */
@Entity
@Table(name = "trucks")
public class Truck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(insertable = false, updatable = false)
    private UUID id;

    @Column(name = "transporter_id")
    private UUID transporterId;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "capacity_kg")
    private BigDecimal capacityKg;

    /** Nullable: a flatbed has no enclosed volume, so null means "no volume constraint". */
    @Column(name = "capacity_m3")
    private BigDecimal capacityM3;

    @Column(name = "current_city")
    private String currentCity;

    @Column(name = "available_from")
    private OffsetDateTime availableFrom;

    @Column(name = "available_until")
    private OffsetDateTime availableUntil;

    @Column(insertable = false)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Truck() {
    }

    public Truck(UUID transporterId, String vehicleType, BigDecimal capacityKg,
            BigDecimal capacityM3, String currentCity, OffsetDateTime availableFrom,
            OffsetDateTime availableUntil) {
        this.transporterId = transporterId;
        this.vehicleType = vehicleType;
        this.capacityKg = capacityKg;
        this.capacityM3 = capacityM3;
        this.currentCity = currentCity;
        this.availableFrom = availableFrom;
        this.availableUntil = availableUntil;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransporterId() {
        return transporterId;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public BigDecimal getCapacityKg() {
        return capacityKg;
    }

    public BigDecimal getCapacityM3() {
        return capacityM3;
    }

    public String getCurrentCity() {
        return currentCity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
