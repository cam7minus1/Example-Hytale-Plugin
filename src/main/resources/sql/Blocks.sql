DROP TABLE IF EXISTS reinforced_blocks;

CREATE TABLE reinforced_blocks (
   world VARCHAR(255) NOT NULL,
   x INT NOT NULL,
   y INT NOT NULL,
   z INT NOT NULL,
   health INT NOT NULL DEFAULT 3,
   PRIMARY KEY (world, x, y, z)
);