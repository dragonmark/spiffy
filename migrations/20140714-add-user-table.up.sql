CREATE TABLE users (
       id BIGINT IDENTITY PRIMARY KEY,	   
       first_name VARCHAR(255),
       last_name VARCHAR(255),
       email VARCHAR(255) UNIQUE,
       passwd VARCHAR(255),
       
       	   );

CREATE INDEX users_email ON users (email);
