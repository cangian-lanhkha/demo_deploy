package fit.hutech.HuynhLeHongXuyen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
                return (web) -> web.ignoring()
                                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/webjars/**");
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/register", "/error").permitAll()
                                                .requestMatchers("/actuator/health", "/actuator/info", "/sitemap.xml").permitAll()
                                                // API: GET public, write ops require ADMIN
                                                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                                                .requestMatchers("/books", "/books/{id}").permitAll()
                                                .requestMatchers("/categories").permitAll()
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/books/add", "/books/edit/**", "/books/delete/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers("/payment/momo/callback", "/payment/momo/ipn",
                                                                "/payment/momo/result",
                                                                "/payment/vnpay/callback", "/payment/vnpay/result",
                                                                "/payment/zalopay/callback", "/payment/zalopay/result",
                                                                "/payment/cod/result")
                                                .permitAll()
                                                .requestMatchers("/payment/**").authenticated()
                                                .requestMatchers("/orders/**").authenticated()
                                                .requestMatchers("/notifications/**").authenticated()
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .defaultSuccessUrl("/", true)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .permitAll())
                                // CSRF: only disable for MoMo IPN webhook (external callback)
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/payment/momo/ipn",
                                                                "/payment/zalopay/callback"))
                                // Security headers
                                .headers(headers -> headers
                                                .contentTypeOptions(opt -> {})
                                                .frameOptions(frame -> frame.deny())
                                                .xssProtection(xss -> xss.headerValue(
                                                                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                                                .httpStrictTransportSecurity(hsts -> hsts
                                                                .includeSubDomains(true)
                                                                .maxAgeInSeconds(31536000)));

                return http.build();
        }
}
