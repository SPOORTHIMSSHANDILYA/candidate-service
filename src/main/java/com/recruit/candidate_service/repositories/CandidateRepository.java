package com.recruit.candidate_service.repositories;

import com.recruit.candidate_service.entities.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> , JpaSpecificationExecutor<Candidate> {

    boolean existsByEmail(String email);

}
