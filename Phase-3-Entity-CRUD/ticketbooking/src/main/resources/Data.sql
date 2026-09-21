-- users
INSERT INTO person (username, password_hash, role) VALUES
  ('admin', '$2a$10$b2An/pwVbaGN5/upih74he3X24zdwl6RaSbgPVtnwwDNIzctAxblO', 'ADMIN'),
  ('rahul', '$2a$10$jCuOFmB3qkWdCHX42xwLteen7765WxENz/CvS3InwETRR0oVPNfy.', 'USER'),
  ('priya', '$2a$10$6zucMa0VmjZE4b5arN0SiejV2ARt.k4lKhkD1o3lhJBcdPvwKyMtO', 'USER')
ON CONFLICT (username) DO UPDATE
  SET password_hash = EXCLUDED.password_hash, role = EXCLUDED.role;

-- events (added only if the name is not already there)
INSERT INTO events (name, venue, event_date, seat_count)
SELECT v.name, v.venue, v.event_date, 100
FROM (VALUES
  ('Rock Night',          'Pune Arena',         TIMESTAMP '2026-12-01 19:30:00'),
  ('Stand-up Comedy',     'Mumbai Comedy Club', TIMESTAMP '2026-12-15 20:00:00'),
  ('Classical Evening',   'Bangalore Hall',     TIMESTAMP '2027-01-10 18:00:00')
) AS v(name, venue, event_date)
WHERE NOT EXISTS (SELECT 1 FROM events e WHERE e.name = v.name);

-- seats A1..J10 for every event that has no seats yet
INSERT INTO seats (event_id, label, status)
SELECT e.id, r.row_letter || n.num::text, 'AVAILABLE'
FROM events e
CROSS JOIN (SELECT chr(64 + g) AS row_letter FROM generate_series(1, 10) AS g) r
CROSS JOIN generate_series(1, 10) AS n(num)
WHERE NOT EXISTS (SELECT 1 FROM seats s WHERE s.event_id = e.id);