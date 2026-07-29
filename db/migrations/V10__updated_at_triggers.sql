-- V10: keep updated_at honest on every mutable table.
--
-- users, trucks and loads each carry an updated_at column that nothing was maintaining, so
-- after any edit it still reported the creation time. A timestamp that looks authoritative
-- while being wrong is worse than no column at all: the admin console (#17) would show a
-- stale "last modified" and no one would have reason to doubt it.
--
-- Done in the database rather than left to each service because there will be several
-- writers (the API, the seed script in #7, admin actions in #17) and a convention only one
-- of them follows is not a guarantee. This is an integrity property of the row, not a
-- domain rule, which is the line drawn in ADR-1.

CREATE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_set_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trucks_set_updated_at
    BEFORE UPDATE ON trucks
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER loads_set_updated_at
    BEFORE UPDATE ON loads
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
