create table bigint_table (bigint_column bigint);
create table bigserial_table (bigserial_column bigserial);
create table bit_table (bit_column bit);
create table bit_varying_table (bit_varying_column bit varying);
create table boolean_table (boolean_column boolean);
create table character_varying_table (character_varying_column character varying);
create table character_table (character_column character);
create table date_table (date_column date);
create table double_precision_table (double_precision_column double precision);
create table integer_table (integer_column integer);
create table money_table (money_column money);
create table real_table (real_column real);
create table smallint_table (smallint_column smallint);
create table serial_table (serial_column serial);
create table text_table (text_column text);
create table time_table (time_column time);
create table timestamp_table (timestamp_column timestamp);
create table int8_table (int8_column int8);
create table serial8_table (serial8_column serial8);
create table varbit_table (varbit_column varbit);
create table bool_table (bool_column bool);
create table varchar_table (varchar_column varchar(100));
create table char_table (char_column char);
create table float8_table (float8_column float8);
create table int_table (int_column int);
create table int4_table (int4_column int4);
create table numeric_table (numeric_column numeric);
create table decimal_table (decimal_column decimal);
create table float4_table (float4_column float4);
create table int2_table (int2_column int2);
create table serial4_table (serial4_column serial4);
create table timetz_table (timetz_column timetz);
create table timestamptz_table (timestamptz_column timestamptz);
create table bytea_table (bytea_column bytea);
create table oid_table (oid_column oid);

CREATE OR REPLACE FUNCTION FUNC_SIMPLETYPE_PARAM(
  param1 IN INTEGER) RETURNS INTEGER 
AS $$
BEGIN
  RETURN 20;
END;
$$ language plpgsql;
