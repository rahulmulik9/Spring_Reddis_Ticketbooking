INSERT INTO person (username, password_hash, role) VALUES
  ('admin', '$2a$10$udZNueuL9In/05fFm0i5dOcEi41uu7zddU32LlJCV7ZFUO2UQlSQi', 'ADMIN'),
  ('rahul', '$2a$10$mpGu0QFuA9/pm4YdPS2R.en.6dqYzMzJpvqf0hwhe8ReluwGdx6iW', 'USER')
ON CONFLICT (username) DO NOTHING;