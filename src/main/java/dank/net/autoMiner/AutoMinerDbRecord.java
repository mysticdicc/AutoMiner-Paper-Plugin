package dank.net.autoMiner;

import org.bukkit.Location;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class AutoMinerDbRecord {
    private Long id;
    private String username;
    private UUID userId;
    private Location location;
    private MinerType minerType;

    public AutoMinerDbRecord() {
    }

    public AutoMinerDbRecord(
            final Long id,
            final String username,
            final UUID userId,
            final Location location,
            final MinerType minerType
    ) {
        this.id = id;
        this.username = username;
        this.userId = userId;
        this.location = location;
        this.minerType = minerType;
    }

    public AutoMinerDbRecord(
            final String username,
            final UUID userId,
            final Location location,
            final MinerType minerType
    ) {
        this(null, username, userId, location, minerType);
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public void setUserId(final UUID userId) {
        this.userId = userId;
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(final Location location) {
        this.location = location;
    }

    public MinerType getMinerType() {
        return this.minerType;
    }

    public void setMinerType(final MinerType minerType) {
        this.minerType = minerType;
    }

    public int getMinerSpeed() {
        return Objects.requireNonNull(this.minerType, "minerType must be set").getSpeed();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AutoMinerDbRecord that)) {
            return false;
        }

        if (this.id != null && that.id != null) {
            return this.id.equals(that.id);
        }

        return Objects.equals(this.username, that.username)
                && Objects.equals(this.userId, that.userId)
                && Objects.equals(this.location, that.location)
                && this.minerType == that.minerType;
    }

    @Override
    public int hashCode() {
        if (this.id != null) {
            return Objects.hash(this.id);
        }

        return Objects.hash(this.username, this.userId, this.location, this.minerType);
    }

    public enum MinerType {
        WOOD(1),
        STONE(2),
        COPPER(3),
        IRON(4),
        GOLD(5),
        DIAMOND(6);

        private final int speed;

        MinerType(final int speed) {
            this.speed = speed;
        }

        public int getSpeed() {
            return this.speed;
        }

        public static Optional<MinerType> fromName(final String value) {
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }

            final String normalized = value.trim().toUpperCase(Locale.ROOT);
            for (final MinerType minerType : values()) {
                if (minerType.name().equals(normalized)) {
                    return Optional.of(minerType);
                }
            }

            return Optional.empty();
        }
    }
}
