package com.recruit.candidate_service.repositories;

import com.recruit.candidate_service.entities.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CandidateRepository extends JpaRepository<Candidate, Long> , JpaSpecificationExecutor<Candidate> {

    boolean existsByEmail(String email);

}
