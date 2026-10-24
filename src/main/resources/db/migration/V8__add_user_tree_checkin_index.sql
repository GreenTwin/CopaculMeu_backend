-- Migration to add composite index for 24-hour user checkin cooldown lookups
CREATE INDEX IF NOT EXISTS idx_tree_checkins_user_tree_cooldown ON tree_checkins(tree_id, user_id, is_valid, created_at DESC);
