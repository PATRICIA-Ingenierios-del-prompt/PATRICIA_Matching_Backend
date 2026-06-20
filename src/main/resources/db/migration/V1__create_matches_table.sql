CREATE TABLE matches (
    id                     UUID PRIMARY KEY,
    usuario_menor_id       UUID NOT NULL,
    usuario_mayor_id       UUID NOT NULL,
    score_intereses        DOUBLE PRECISION NOT NULL,
    score_academico        DOUBLE PRECISION NOT NULL,
    score_disponibilidad   DOUBLE PRECISION NOT NULL,
    confirmado_en          TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_matches_par_usuarios UNIQUE (usuario_menor_id, usuario_mayor_id),
    CONSTRAINT ck_matches_orden_par CHECK (usuario_menor_id < usuario_mayor_id)
);

CREATE INDEX idx_matches_usuario_menor_id ON matches (usuario_menor_id);
CREATE INDEX idx_matches_usuario_mayor_id ON matches (usuario_mayor_id);
