-- V3__Seed_regions.sql

-- Insert a small set of US states for testing
INSERT INTO region (region_pk, geo_level, geo_id, name, parent_region_pk, centroid_lat, centroid_lon) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'STATE', '20', 'Kansas', NULL, 38.5266, -96.7265),
('550e8400-e29b-41d4-a716-446655440002', 'STATE', '06', 'California', NULL, 36.7783, -119.4179),
('550e8400-e29b-41d4-a716-446655440003', 'STATE', '48', 'Texas', NULL, 31.9686, -99.9018),
('550e8400-e29b-41d4-a716-446655440004', 'STATE', '36', 'New York', NULL, 42.1657, -74.9481),
('550e8400-e29b-41d4-a716-446655440005', 'STATE', '12', 'Florida', NULL, 27.7663, -81.6868);