DROP TABLE IF EXISTS reinforced_blocks;

CREATE TABLE reinforced_blocks (
   PRIMARY KEY (world, x, y, z)
   world TEXT NOT NULL,
   x INTEGER NOT NULL,
   y INTEGER NOT NULL,
   z INTEGER NOT NULL,
   health INTEGER NOT NULL DEFAULT 5,
);