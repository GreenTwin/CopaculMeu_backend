CREATE TABLE IF NOT EXISTS email_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_email_templates_name ON email_templates (name);

-- Seed default template for registration confirmation code
INSERT INTO email_templates (id, name, subject, body, description, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'REGISTRATION_CONFIRMATION',
    'Codul tau de activare Copacul Meu',
    'Buna {{name}},

Iti multumim ca te-ai inregistrat pe platforma Copacul Meu!
Codul tau de confirmare este: {{code}}

Dupa confirmare, contul tau va fi verificat de un administrator inainte de activare.

Echipa Copacul Meu',
    'Email template for sending activation/confirmation code upon registration',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (name) DO NOTHING;

-- Seed default template for forgot password / password reset
INSERT INTO email_templates (id, name, subject, body, description, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'FORGOT_PASSWORD',
    'Resetare parola Copacul Meu',
    'Buna {{name}},

Ai solicitat resetarea parolei pentru contul Copacul Meu asociat acestei adrese de email.
Codul tau de resetare a parolei este: {{code}}

Acest cod este valabil 24 de ore. Daca nu ai solicitat aceasta resetare, te rugam sa ignori acest mesaj.

Echipa Copacul Meu',
    'Email template for forgot password / password reset code',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (name) DO NOTHING;
