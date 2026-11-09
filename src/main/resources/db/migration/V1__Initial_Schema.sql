CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE users (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    email varchar(255) NOT NULL,
    name varchar(255) NOT NULL,
    password varchar(255) NOT NULL,
    role varchar(255) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'TOURIST', 'GUIDE'))
);

CREATE TABLE attractions (
    id uuid NOT NULL,
    available_spots integer NOT NULL,
    description varchar(2000),
    location geometry(Point, 4326),
    price numeric(10, 2) NOT NULL,
    title varchar(255) NOT NULL,
    version integer NOT NULL,
    guide_id uuid NOT NULL,
    rating_average double precision NOT NULL,
    review_count integer NOT NULL,
    CONSTRAINT pk_attractions PRIMARY KEY (id),
    CONSTRAINT fk_attractions_guide FOREIGN KEY (guide_id) REFERENCES users (id)
);

CREATE TABLE reservations (
    id uuid NOT NULL,
    cancelled_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone,
    reserved_for timestamp(6) without time zone NOT NULL,
    status varchar(255) NOT NULL,
    attraction_id uuid NOT NULL,
    tourist_id uuid NOT NULL,
    CONSTRAINT pk_reservations PRIMARY KEY (id),
    CONSTRAINT fk_reservations_attraction FOREIGN KEY (attraction_id) REFERENCES attractions (id),
    CONSTRAINT fk_reservations_tourist FOREIGN KEY (tourist_id) REFERENCES users (id),
    CONSTRAINT reservations_status_check CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'))
);

CREATE TABLE reviews (
    id uuid NOT NULL,
    comment varchar(500),
    created_at timestamp(6) without time zone,
    rating integer NOT NULL,
    attraction_id uuid NOT NULL,
    tourist_id uuid NOT NULL,
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT fk_reviews_attraction FOREIGN KEY (attraction_id) REFERENCES attractions (id),
    CONSTRAINT fk_reviews_tourist FOREIGN KEY (tourist_id) REFERENCES users (id),
    CONSTRAINT reviews_rating_check CHECK (rating >= 1 AND rating <= 5)
);

CREATE TABLE refresh_tokens (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    expires_at timestamp(6) without time zone NOT NULL,
    token varchar(255) NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE token_blacklist (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    expires_at timestamp(6) without time zone NOT NULL,
    token varchar(1000) NOT NULL,
    CONSTRAINT pk_token_blacklist PRIMARY KEY (id),
    CONSTRAINT uk_token_blacklist_token UNIQUE (token)
);
