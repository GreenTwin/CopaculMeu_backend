-- Migration to add status and proposal review tracking to trees table
ALTER TABLE trees ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE trees ADD COLUMN IF NOT EXISTS proposed_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE trees ADD COLUMN IF NOT EXISTS reviewed_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE trees ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ;
ALTER TABLE trees ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

CREATE INDEX IF NOT EXISTS idx_trees_status ON trees(status);
CREATE INDEX IF NOT EXISTS idx_trees_proposed_by ON trees(proposed_by_user_id);
