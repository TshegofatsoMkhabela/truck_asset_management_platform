-- Local development seed data (#7). Walks the whole demo journey brief section 1.2
-- describes, so #19's User/Demo Guide has real data to script against instead of an
-- empty database: registration → posting → matching → acceptance → tracking → rating →
-- admin. Runs once, after every migration (see docker-compose.yml's mount ordering).
--
-- Fixed literal UUIDs, not generated ones: this is static, hand-maintained sample data
-- (a deliberate choice, see ADR-2-style reasoning in the PR), and a reviewer or the
-- README can reference a specific row ("the accepted match") by a stable id across runs.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Real bcrypt hashes, generated live by Postgres's own pgcrypto extension
-- (crypt(..., gen_salt('bf'))) rather than a hash pasted in as a literal. This produces
-- the same $2a$/$2b$ format Spring Security's BCryptPasswordEncoder reads, so #9's login
-- can verify against these rows with no format conversion. All three demo accounts share
-- one password so the README only needs to document it once.
-- Demo password for every seeded account: TampDemo2026!
--
-- gen_salt('bf') with no explicit argument defaults to bcrypt cost factor 6, weaker than
-- the 10-12 typically recommended for real credentials. Fine for three throwaway demo
-- rows; #9 should choose its own work factor deliberately for real user registration
-- rather than copying this call.

INSERT INTO users (id, full_name, email, password_hash, role, compliance_status) VALUES
    ('00000000-0000-7000-8000-000000000001', 'Naledi Dlamini', 'owner@tamp.example',
        crypt('TampDemo2026!', gen_salt('bf')), 'FREIGHT_OWNER', 'APPROVED'),
    ('00000000-0000-7000-8000-000000000002', 'Sipho Mokoena', 'transporter@tamp.example',
        crypt('TampDemo2026!', gen_salt('bf')), 'TRANSPORTER', 'APPROVED'),
    ('00000000-0000-7000-8000-000000000003', 'Admin User', 'admin@tamp.example',
        crypt('TampDemo2026!', gen_salt('bf')), 'ADMIN', 'APPROVED');

-- One load already matched and delivered (the completed journey), one still open (FR-03
-- "create and view cargo loads" has something to view beyond the completed example).
INSERT INTO loads (id, owner_id, origin_city, destination_city, cargo_type, weight_kg,
                    volume_m3, pickup_window_start, pickup_window_end, status) VALUES
    ('00000000-0000-7000-8000-000000000010', '00000000-0000-7000-8000-000000000001',
        'Johannesburg', 'Durban', 'GENERAL', 12000.00, 30.00,
        now() - interval '2 days', now() - interval '1 day', 'DELIVERED'),
    ('00000000-0000-7000-8000-000000000011', '00000000-0000-7000-8000-000000000001',
        'Cape Town', 'Bloemfontein', 'REFRIGERATED', 8000.00, 18.00,
        now() + interval '1 day', now() + interval '3 days', 'OPEN');

INSERT INTO trucks (id, transporter_id, vehicle_type, capacity_kg, capacity_m3,
                     current_city, available_from, available_until, status) VALUES
    ('00000000-0000-7000-8000-000000000020', '00000000-0000-7000-8000-000000000002',
        'FLATBED', 20000.00, 45.00, 'Johannesburg',
        now() - interval '3 days', now() + interval '1 day', 'AVAILABLE'),
    ('00000000-0000-7000-8000-000000000021', '00000000-0000-7000-8000-000000000002',
        'REFRIGERATED', 10000.00, 25.00, 'Cape Town',
        now() + interval '1 day', now() + interval '4 days', 'AVAILABLE');

-- The one accepted match, decided by the Freight Owner (matches_decision_consistency_check
-- requires status/decided_at/decided_by to move together).
INSERT INTO matches (id, load_id, truck_id, score, reasons, status, decided_at, decided_by) VALUES
    ('00000000-0000-7000-8000-000000000030',
        '00000000-0000-7000-8000-000000000010', '00000000-0000-7000-8000-000000000020',
        92.50, '["capacity sufficient", "same origin city", "availability windows overlap"]'::jsonb,
        'ACCEPTED', now() - interval '2 days', '00000000-0000-7000-8000-000000000001');

INSERT INTO receipts (match_id, decision, actor_id, ip_address, user_agent) VALUES
    ('00000000-0000-7000-8000-000000000030', 'ACCEPTED',
        '00000000-0000-7000-8000-000000000001', '102.65.10.20', 'Mozilla/5.0 (demo seed)');

-- Trip progress across the pickup window, oldest first (FR-08).
INSERT INTO tracking_events (match_id, latitude, longitude, status, occurred_at) VALUES
    ('00000000-0000-7000-8000-000000000030', -26.204103, 28.047304, 'DISPATCHED', now() - interval '2 days'),
    ('00000000-0000-7000-8000-000000000030', -28.748000, 24.763000, 'IN_TRANSIT', now() - interval '36 hours'),
    ('00000000-0000-7000-8000-000000000030', -29.858680, 31.021840, 'DELIVERED', now() - interval '1 day');

-- Both parties rate each other after the completed trip (FR-09).
INSERT INTO ratings (match_id, rater_id, ratee_id, score, comment) VALUES
    ('00000000-0000-7000-8000-000000000030', '00000000-0000-7000-8000-000000000001',
        '00000000-0000-7000-8000-000000000002', 5, 'On time and careful with the cargo.'),
    ('00000000-0000-7000-8000-000000000030', '00000000-0000-7000-8000-000000000002',
        '00000000-0000-7000-8000-000000000001', 4, 'Smooth pickup, clear instructions.');

-- One open dispute so the admin console (#18) has something in its queue on first login
-- (FR-10), and one pending compliance document so the same login has paperwork to review
-- (FR-02).
INSERT INTO disputes (match_id, raised_by, description) VALUES
    ('00000000-0000-7000-8000-000000000030', '00000000-0000-7000-8000-000000000001',
        'Delivery arrived four hours later than the confirmed window.');

INSERT INTO compliance_documents (user_id, document_type, file_name) VALUES
    ('00000000-0000-7000-8000-000000000002', 'OPERATOR_LICENCE', 'operator-licence-scan.pdf');
