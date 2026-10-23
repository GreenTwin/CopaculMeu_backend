CREATE TABLE IF NOT EXISTS trees (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    species VARCHAR(255) NOT NULL,
    health_level VARCHAR(50) NOT NULL,
    watering_level VARCHAR(50) NOT NULL,
    last_watered_at TIMESTAMP WITH TIME ZONE,
    height DOUBLE PRECISION,
    age INTEGER,
    trunk_circumference DOUBLE PRECISION,
    location_description VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_tree_latitude CHECK (latitude >= -90.0 AND latitude <= 90.0),
    CONSTRAINT chk_tree_longitude CHECK (longitude >= -180.0 AND longitude <= 180.0),
    CONSTRAINT chk_tree_height CHECK (height IS NULL OR height >= 0),
    CONSTRAINT chk_tree_age CHECK (age IS NULL OR age >= 0),
    CONSTRAINT chk_tree_trunk_circumference CHECK (trunk_circumference IS NULL OR trunk_circumference >= 0)
);

CREATE INDEX IF NOT EXISTS idx_trees_coordinates ON trees (latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_trees_species ON trees (species);
CREATE INDEX IF NOT EXISTS idx_trees_health_level ON trees (health_level);
