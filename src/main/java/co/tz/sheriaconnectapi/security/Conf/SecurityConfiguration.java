package co.tz.sheriaconnectapi.security.Conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.security.Jwt.JwtAuthenticationFilter;
import co.tz.sheriaconnectapi.security.UserDetails.CustomUserDetailsService;
import co.tz.sheriaconnectapi.repositories.AuthSessionRepository;
import co.tz.sheriaconnectapi.security.Access.ScopedAuthorityService;
import co.tz.sheriaconnectapi.security.Handlers.ApiAccessDeniedHandler;
import co.tz.sheriaconnectapi.security.Handlers.ApiAuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;
    private final AuthSessionRepository authSessionRepository;
    private final ScopedAuthorityService scopedAuthorityService;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;

    public SecurityConfiguration(
            UserRepository userRepository,
            CustomUserDetailsService userDetailsService,
            AuthSessionRepository authSessionRepository,
            ScopedAuthorityService scopedAuthorityService,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler
    ) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.authSessionRepository = authSessionRepository;
        this.scopedAuthorityService = scopedAuthorityService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    // AuthenticationManager is now obtained via AuthenticationConfiguration
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationProvider using your CustomUserDetailsService
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }




    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("🔥 CUSTOM SECURITY FILTER CHAIN LOADED");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/").permitAll()
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/auth/verify-email").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/staff-invitations/validate").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/staff-invitations/accept").permitAll()
                        .requestMatchers("/auth/login", "/auth/refresh", "/auth/logout", "/auth/register", "/auth/password-reset/request", "/auth/password-reset/confirm").permitAll()
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/incident-reports/mine").authenticated()
                        .requestMatchers(HttpMethod.POST, "/incident-reports").permitAll()
                        .requestMatchers(HttpMethod.GET, "/incident-reports/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/incident-reports/*/evidence").permitAll()
                        .requestMatchers(HttpMethod.GET, "/incident-reports/*/evidence/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/stories/mine").authenticated()
                        .requestMatchers(HttpMethod.GET, "/stories/bookmarks").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/stories/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/stories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/stories/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/stories/*/reports").permitAll()
                        .requestMatchers(HttpMethod.GET, "/knowledge/articles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/knowledge/articles/*").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(authenticationJwtFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter authenticationJwtFilter() {
        return new JwtAuthenticationFilter(
                userRepository,
                authSessionRepository,
                scopedAuthorityService,
                authenticationEntryPoint,
                accessDeniedHandler
        );
    }
}
