package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends AbstractRepository<UserEntity, UUID> {

    Optional<UserEntity> findBySupabaseUserId(String supabaseUserId);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsBySupabaseUserId(String supabaseUserId);
}