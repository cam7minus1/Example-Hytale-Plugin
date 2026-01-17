DROP TABLE IF EXISTS reinforced_blocks;

CREATE TABLE reinforced_blocks (
   world TEXT NOT NULL,
   x INTEGER NOT NULL,
   y INTEGER NOT NULL,
   z INTEGER NOT NULL,
   health INTEGER NOT NULL DEFAULT 5,
   PRIMARY KEY (world, x, y, z)
);