CREATE TABLE district (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    color VARCHAR(7) NOT NULL CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE day_district (
    day DATE PRIMARY KEY,
    district_id BIGINT NOT NULL REFERENCES district(id)
);

-- Initial operational areas, not an official administrative classification.
-- Replace/extend through a future settings screen.
INSERT INTO district(name, color) VALUES
('Fabryczna', '#DBEAFE'),
('Krzyki', '#D1FAE5'),
('Psie Pole', '#FEF3C7'),
('Stare Miasto', '#FCE7F3'),
('Śródmieście', '#EDE9FE');
