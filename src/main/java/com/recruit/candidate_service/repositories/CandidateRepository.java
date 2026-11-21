package com.recruit.candidate_service.repositories;

import com.recruit.candidate_service.entities.Candidate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> , JpaSpecificationExecutor<Candidate> {

    boolean existsByEmail(String email);

    @Query("select distinct s from Candidate c join c.skills s where s like :pattern escape '!' order by s")
    List<String> findSkills(@Param("pattern") String pattern, Pageable pageable);

}
