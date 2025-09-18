ALTER TABLE ocpp_tag
    ADD COLUMN creation_origin VARCHAR(50)  NOT NULL DEFAULT 'UI' COMMENT 'Origin of the tag (e.g., UI, OCPI)',
    ADD COLUMN issuer            VARCHAR(64)  NULL COMMENT 'OCPI field: Issuing company',
    ADD COLUMN type              VARCHAR(50)  NULL COMMENT 'OCPI field: Type of the token (e.g., RFID, APP_USER)',
    ADD COLUMN contract_id       VARCHAR(36)  NULL COMMENT 'OCPI field: Uniquely identifies the EV Driver contract token',
    ADD COLUMN language          VARCHAR(2)   NULL COMMENT 'OCPI field: Language Code ISO 639-1',
    ADD COLUMN visual_number     VARCHAR(64)  NULL COMMENT 'OCPI field: Visual readable number/identification',
    ADD COLUMN last_updated      TIMESTAMP(6) NULL COMMENT 'OCPI field: Last updated';

CREATE OR REPLACE VIEW `ocpp_tag_activity` AS select
    `o`.`ocpp_tag_pk` AS `ocpp_tag_pk`,
    `o`.`id_tag` AS `id_tag`,
    `o`.`parent_id_tag` AS `parent_id_tag`,
    `o`.`expiry_date` AS `expiry_date`,
    `o`.`max_active_transaction_count` AS `max_active_transaction_count`,
    `o`.`note` AS `note`,
    `o`.`creation_origin` AS `creation_origin`,
    `o`.`issuer` AS `issuer`,
    `o`.`type` AS `type`,
    `o`.`contract_id` AS `contract_id`,
    `o`.`language` AS `language`,
    `o`.`visual_number` AS `visual_number`,
    `o`.`last_updated` AS `last_updated`,
    count(`t`.`id_tag`) AS `active_transaction_count`,
    case when count(`t`.`id_tag`) > 0 then 1 else 0 end AS `in_transaction`,
    case when `o`.`max_active_transaction_count` = 0 then 1 else 0 end AS `blocked`
from (`ocpp_tag` `o` left join `transaction` `t` on(
    `o`.`id_tag` = `t`.`id_tag` and
    `t`.`stop_timestamp` is null and
    `t`.`stop_value` is null))
group by `o`.`ocpp_tag_pk`;
