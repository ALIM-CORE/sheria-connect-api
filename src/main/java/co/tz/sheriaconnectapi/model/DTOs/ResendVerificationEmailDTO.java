package co.tz.sheriaconnectapi.model.DTOs;

public class ResendVerificationEmailDTO {
    private String email;

    public ResendVerificationEmailDTO() {
    }

    public ResendVerificationEmailDTO(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}