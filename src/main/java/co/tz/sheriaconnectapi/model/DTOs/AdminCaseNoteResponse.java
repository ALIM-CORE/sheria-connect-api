package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.AdminCaseNote;
import lombok.Getter;

import java.time.Instant;

@Getter
public class AdminCaseNoteResponse {
    private final Long id;
    private final String note;
    private final String adminEmail;
    private final Instant createdAt;

    public AdminCaseNoteResponse(AdminCaseNote note) {
        this.id = note.getId();
        this.note = note.getNote();
        this.adminEmail = note.getAdminUser() == null
                ? null
                : note.getAdminUser().getEmail();
        this.createdAt = note.getCreatedAt();
    }
}
