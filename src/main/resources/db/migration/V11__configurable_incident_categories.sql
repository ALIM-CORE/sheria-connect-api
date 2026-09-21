CREATE TABLE incident_categories (
    code VARCHAR(64) PRIMARY KEY,
    name_en VARCHAR(180) NOT NULL,
    name_sw VARCHAR(180),
    body_en TEXT,
    body_sw TEXT,
    selectable BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incident_categories_selectable_order
    ON incident_categories(selectable, sort_order, code);

INSERT INTO incident_categories
    (code, name_en, name_sw, body_en, body_sw, selectable, sort_order)
VALUES
    (
        'GENDER_BASED_VIOLENCE',
        'Gender-Based Violence (GBV) & Violence Against Women',
        'Ukatili wa Kijinsia (GBV) na Ukatili Dhidi ya Wanawake',
        'Domestic violence, sexual violence, harassment, economic abuse, forced marriage, and other forms of abuse.',
        'Ukatili wa majumbani, ukatili wa kijinsia, unyanyasaji, ukandamizaji wa kiuchumi, ndoa za mkondo (ndoa za lazima), na aina nyingine za unyanyasaji.',
        TRUE,
        10
    ),
    (
        'CHILD_PROTECTION',
        'Child Protection & Child Abuse',
        'Ulinzi wa Mtoto na Ukatili Dhidi ya Watoto',
        'Child sexual abuse, physical abuse, neglect, exploitation, child marriage, and other violations of children''s rights.',
        'Ukatili wa kijinsia dhidi ya watoto, ukatili wa kimwili, utelekezaji, utumikishwaji, ndoa za utotoni, na ukiukaji mwingine wa haki za watoto.',
        TRUE,
        20
    ),
    (
        'DIGITAL_SAFETY',
        'Digital Safety & Online Violence',
        'Ulinzi Mtandaoni na Ukatili wa Kidigitali',
        'Cyberbullying, online sexual harassment, digital stalking, image-based abuse, online threats, privacy violations, and online exploitation of women and children.',
        'Unyanyasaji wa mtandaoni (cyberbullying), unyanyasaji wa kijinsia mtandaoni, ufuatiliaji wa kidijitali wa kinyemela, usambazaji wa picha au video za utupu bila ridhaa, vitisho vya mtandaoni, ukiukaji wa faragha, na utumikishwaji wa wanawake na watoto mtandaoni.',
        TRUE,
        30
    ),
    ('UNLAWFUL_ARREST', 'Unlawful arrest', 'Kukamatwa kinyume cha sheria', NULL, NULL, FALSE, 110),
    ('POLICE_BRUTALITY', 'Police brutality', 'Ukatili wa polisi', NULL, NULL, FALSE, 120),
    ('DOMESTIC_VIOLENCE', 'Domestic violence', 'Ukatili wa nyumbani', NULL, NULL, FALSE, 130),
    ('LAND_RIGHTS', 'Land rights', 'Haki za ardhi', NULL, NULL, FALSE, 140),
    ('DISCRIMINATION', 'Discrimination', 'Ubaguzi', NULL, NULL, FALSE, 150),
    ('TORTURE', 'Torture', 'Mateso', NULL, NULL, FALSE, 160),
    ('ENFORCED_DISAPPEARANCE', 'Enforced disappearance', 'Kutoweshwa kwa nguvu', NULL, NULL, FALSE, 170),
    ('FREEDOM_OF_SPEECH', 'Freedom of speech', 'Uhuru wa kujieleza', NULL, NULL, FALSE, 180),
    ('OTHER', 'Other', 'Nyingine', NULL, NULL, FALSE, 190)
ON CONFLICT (code) DO NOTHING;
