package co.tz.sheriaconnectapi.model.DTOs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RefreshInput {
    HttpServletRequest request;
    HttpServletResponse response;
    RefreshTokenRequest body;
}
