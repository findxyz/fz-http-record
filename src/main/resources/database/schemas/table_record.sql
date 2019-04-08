CREATE TABLE t_record (
  id BIGINT PRIMARY KEY,
  method VARCHAR(100),
  url VARCHAR(300),
  request CLOB(10K),
  response CLOB(10K)
);
