CREATE TABLE IF NOT EXISTS profile_data
(
    uuid BINARY(16)           NOT NULL,
    level_up_messages BOOLEAN NOT NULL,
    PRIMARY KEY (uuid)
);
CREATE TABLE IF NOT EXISTS selected_pets
(
    uuid BINARY(16)    NOT NULL,
    slot TINYINT       NOT NULL,
    petID VARCHAR(32)  NOT NULL,
    xp DOUBLE DEFAULT 0,
    `timestamp` BIGINT NOT NULL,
    PRIMARY KEY (uuid, slot)
);