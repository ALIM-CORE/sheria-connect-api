package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.MatchingRequestStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMatchingRequestStatusRequest {
    private MatchingRequestStatus status;
    private String notes;
}
