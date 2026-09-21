-- Incident category validity is enforced through incident_categories and the
-- application validation services. Older databases may still contain enum
-- check constraints generated before category codes became configurable.
DO $$
DECLARE
    legacy_constraint RECORD;
BEGIN
    FOR legacy_constraint IN
        SELECT DISTINCT
            namespace.nspname AS schema_name,
            relation.relname AS table_name,
            constraint_definition.conname AS constraint_name
        FROM pg_constraint constraint_definition
        JOIN pg_class relation
            ON relation.oid = constraint_definition.conrelid
        JOIN pg_namespace namespace
            ON namespace.oid = relation.relnamespace
        JOIN pg_attribute attribute_definition
            ON attribute_definition.attrelid = relation.oid
            AND attribute_definition.attnum = ANY (constraint_definition.conkey)
        WHERE constraint_definition.contype = 'c'
          AND relation.relname IN ('incident_reports', 'provider_profile_specialties')
          AND attribute_definition.attname = 'incident_type'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT %I',
            legacy_constraint.schema_name,
            legacy_constraint.table_name,
            legacy_constraint.constraint_name
        );
    END LOOP;
END;
$$;
