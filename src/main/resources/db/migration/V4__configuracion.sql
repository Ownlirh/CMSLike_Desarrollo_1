CREATE TABLE public.configuracion (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    logo VARCHAR(500),
    color VARCHAR(7) NOT NULL
);

INSERT INTO public.configuracion (id, nombre, color) VALUES (1, 'CMSLike', '#234c47');
