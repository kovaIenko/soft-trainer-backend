
drop table if exists chat cascade;

drop table if exists flows cascade;

drop table if exists messages cascade;

drop table if exists users cascade;

drop table if exists characters cascade;



ALTER TABLE flows ALTER COLUMN dtype TYPE text;


ALTER TABLE flows ALTER COLUMN text TYPE VARCHAR(1000);


SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'options';

SELECT f FROM flows f WHERE f.name = 'Giving feedbacks by kovalenko' ORDER BY f.order_number LIMIT 10
