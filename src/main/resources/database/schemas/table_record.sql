CREATE TABLE t_record (
  id BIGINT PRIMARY KEY,
  method VARCHAR(20),
  url VARCHAR(1000),
  request CLOB(300K),
  response CLOB(300K)
);
