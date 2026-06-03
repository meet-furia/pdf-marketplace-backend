package com.meet.pdf_marketplace.service;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class SupabaseJwtService {

    /**
     * Reads the Supabase user id from the JWT subject claim.
     */
    public String getSupabaseUserId(Jwt jwt) {

        String supabaseUserId = jwt.getSubject();

        if (supabaseUserId == null || supabaseUserId.isBlank()) {
            throw new IllegalArgumentException("Supabase JWT subject is missing");
        }

        return supabaseUserId;
    }

    /**
     * Reads the email claim from the Supabase JWT.
     */
    public String getEmail(Jwt jwt) {

        return jwt.getClaimAsString("email");
    }
}
