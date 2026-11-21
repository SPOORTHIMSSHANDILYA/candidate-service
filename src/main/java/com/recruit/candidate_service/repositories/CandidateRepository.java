package com.recruit.candidate_service.repositories;

import com.recruit.candidate_service.constants.CandidateStatus;
import com.recruit.candidate_service.entities.Candidate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> , JpaSpecificationExecutor<Candidate> {

    interface StatusCount {
        CandidateStatus getStatus();
        long getCount();
    }

    boolean existsByEmail(String email);

    @Query("select c.candidateStatus as status, count(c) as count " +
            "from Candidate c group by c.candidateStatus")
    List<StatusCount> countCandidatesByStatus();

    long countByCreatedAtBetween(Instant since, Instant until);

    /** SELECT COUNT(*) ... WHERE open_to_work = TRUE */
    long countByOpenToWorkTrue();

    @Query("select distinct s from Candidate c join c.skills s where s like :pattern escape '!' order by s")
    List<String> findSkills(@Param("pattern") String pattern, Pageable pageable);

}
