package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends AbstractRepository<UserEntity, UUID> {

    Optional<UserEntity> findByAuth0Id(String auth0Id);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByAuth0Id(String auth0Id);
}