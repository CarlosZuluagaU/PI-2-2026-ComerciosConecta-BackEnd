CREATE TABLE comercio (
                          id serial PRIMARY KEY,
                          nombre varchar,
                          nit varchar,
                          direccion varchar,
                          ciudad varchar(100),
                          telefono varchar,
                          email varchar,
                          created_at timestamp DEFAULT now()
);

CREATE TABLE rol (
                     id serial PRIMARY KEY,
                     nombre varchar,
                     descripcion varchar
);

CREATE TABLE usuario (
                         id serial PRIMARY KEY,
                         comercio_id int REFERENCES comercio(id),
                         nombre varchar,
                         apellido varchar(255),
                         telefono varchar(50),
                         tipo_documento varchar(50),
                         numero_documento varchar(100),
                         fecha_nacimiento varchar(30),
                         ciudad varchar(100),
                         direccion varchar(255),
                         biografia varchar(500),
                         email varchar UNIQUE,
                         password varchar,
                         estado varchar,
                         created_at timestamp DEFAULT now()
);

CREATE TABLE usuario_rol (
                             usuario_id int REFERENCES usuario(id),
                             rol_id int REFERENCES rol(id),
                             PRIMARY KEY(usuario_id, rol_id)
);

CREATE TABLE refresh_token (
                               id serial PRIMARY KEY,
                               usuario_id int REFERENCES usuario(id),
                               token_hash varchar,
                               expires_at timestamp,
                               revoked boolean DEFAULT false,
                               created_at timestamp DEFAULT now()
);

CREATE TABLE cliente (
                         id serial PRIMARY KEY,
                         tipo_documento varchar(50) NOT NULL,
                         numero_documento varchar(100) UNIQUE NOT NULL,
                         nombres varchar(150) NOT NULL,
                         apellidos varchar(150),
                         telefono varchar(50),
                         correo varchar(150),
                         direccion varchar(255),
                         ciudad varchar(100),
                         estado varchar(50) DEFAULT 'Activo',
                         created_at timestamp DEFAULT now()
);

