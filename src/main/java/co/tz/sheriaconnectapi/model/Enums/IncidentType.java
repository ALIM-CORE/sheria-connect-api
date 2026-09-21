package co.tz.sheriaconnectapi.model.Enums;

/**
 * Compatibility catalogue for older server code and clients.
 * Incident category persistence and validation are table-driven.
 */
@Deprecated(forRemoval = false)
public enum IncidentType {
    GENDER_BASED_VIOLENCE,
    CHILD_PROTECTION,
    DIGITAL_SAFETY,
    UNLAWFUL_ARREST,
    POLICE_BRUTALITY,
    DOMESTIC_VIOLENCE,
    LAND_RIGHTS,
    DISCRIMINATION,
    TORTURE,
    ENFORCED_DISAPPEARANCE,
    FREEDOM_OF_SPEECH,
    OTHER
}
