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

/** Maps {@code loads} (db/migrations/V03__loads.sql). */
@Entity
@Table(name = "loads")
public class Load {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(insertable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "origin_city")
    private String originCity;

    @Column(name = "destination_city")
    private String destinationCity;

    @Column(name = "cargo_type")
    private String cargoType;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "volume_m3")
    private BigDecimal volumeM3;

    @Column(name = "pickup_window_start")
    private OffsetDateTime pickupWindowStart;

    @Column(name = "pickup_window_end")
    private OffsetDateTime pickupWindowEnd;

    @Column(insertable = false)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Load() {
    }

    public Load(UUID ownerId, String originCity, String destinationCity, String cargoType,
            BigDecimal weightKg, BigDecimal volumeM3, OffsetDateTime pickupWindowStart,
            OffsetDateTime pickupWindowEnd) {
        this.ownerId = ownerId;
        this.originCity = originCity;
        this.destinationCity = destinationCity;
        this.cargoType = cargoType;
        this.weightKg = weightKg;
        this.volumeM3 = volumeM3;
        this.pickupWindowStart = pickupWindowStart;
        this.pickupWindowEnd = pickupWindowEnd;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOriginCity() {
        return originCity;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public String getCargoType() {
        return cargoType;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public BigDecimal getVolumeM3() {
        return volumeM3;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
