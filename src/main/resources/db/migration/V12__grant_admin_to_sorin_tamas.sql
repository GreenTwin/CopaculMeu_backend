UPDATE users
SET role = 'ADMIN',
    updated_at = CURRENT_TIMESTAMP
WHERE lower(email) = 'tamas.sorin90@gmail.com';
