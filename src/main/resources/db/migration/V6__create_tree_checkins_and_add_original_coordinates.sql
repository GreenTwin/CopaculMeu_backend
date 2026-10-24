-- Migration to add original GPS coordinates to trees and create tree_checkins table
ALTER TABLE trees ADD COLUMN IF NOT EXISTS original_latitude DOUBLE PRECISION;
ALTER TABLE trees ADD COLUMN IF NOT EXISTS original_longitude DOUBLE PRECISION;

-- Backfill original coordinates from existing latitude and longitude
UPDATE trees SET original_latitude = latitude, original_longitude = longitude WHERE original_latitude IS NULL;

ALTER TABLE trees ALTER COLUMN original_latitude SET NOT NULL;
ALTER TABLE trees ALTER COLUMN original_longitude SET NOT NULL;

CREATE TABLE IF NOT EXISTS tree_checkins (
    id UUID PRIMARY KEY,
    tree_id BIGINT NOT NULL REFERENCES trees(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    deviation_meters DOUBLE PRECISION NOT NULL,
    is_valid BOOLEAN NOT NULL,
    watering_level VARCHAR(50) NOT NULL,
    watered BOOLEAN NOT NULL,
    proposed_species VARCHAR(255),
    health_level VARCHAR(50),
    height DOUBLE PRECISION,
    age INTEGER,
    trunk_circumference DOUBLE PRECISION,
    location_description VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tree_checkins_tree_id ON tree_checkins(tree_id);
CREATE INDEX IF NOT EXISTS idx_tree_checkins_user_id ON tree_checkins(user_id);
CREATE INDEX IF NOT EXISTS idx_tree_checkins_created_at ON tree_checkins(created_at);
CREATE INDEX IF NOT EXISTS idx_tree_checkins_valid ON tree_checkins(tree_id, is_valid, created_at);
