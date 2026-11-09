-- 1. Índice espacial funcional GIST compatível com queries que usam CAST(location AS geography)
CREATE INDEX IF NOT EXISTS idx_attractions_location_geog
    ON attractions USING GIST (CAST(location AS geography));

-- 2. Índices B-Tree nas chaves estrangeiras (FKs) para acelerar JOINs e buscas filtradas
CREATE INDEX IF NOT EXISTS idx_attractions_guide_id ON attractions (guide_id);
CREATE INDEX IF NOT EXISTS idx_reservations_tourist_id ON reservations (tourist_id);
CREATE INDEX IF NOT EXISTS idx_reservations_attraction_id ON reservations (attraction_id);
CREATE INDEX IF NOT EXISTS idx_reviews_attraction_id ON reviews (attraction_id);
CREATE INDEX IF NOT EXISTS idx_reviews_tourist_id ON reviews (tourist_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- 3. Índice único parcial para impedir condição de corrida (race condition) em reservas duplicadas pendentes
CREATE UNIQUE INDEX IF NOT EXISTS uk_reservations_pending_tourist_attraction
    ON reservations (tourist_id, attraction_id)
    WHERE status = 'PENDING';
