package com.recruit.candidate_service.entities;

import com.recruit.candidate_service.constants.CandidateStatus;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // MySQL AUTO_INCREMENT
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "current_title", length = 150)
    private String currentTitle;

    @Column(name = "current_company", length = 150)
    private String currentCompany;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @JdbcTypeCode(SqlTypes.TINYINT)                  // column is TINYINT UNSIGNED
    @Column(name = "years_experience")
    private Integer yearsExperience;

    @JdbcTypeCode(SqlTypes.SMALLINT)                 // column is SMALLINT UNSIGNED
    @Column(name = "notice_period_days")
    private Integer noticePeriodDays;

    @Column(name = "open_to_work", nullable = false)
    private boolean openToWork = true;

    /** Record lifecycle: ACTIVE, INACTIVE, ARCHIVED. Stored as text. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)                  // column is VARCHAR(20), not a MySQL ENUM
    @Column(name = "candidate_status", nullable = false, length = 20)
    private CandidateStatus candidateStatus = CandidateStatus.ACTIVE;

    /** Recruiter username who created the record (audit). Set once, never changed. */
    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    /** Recruiter username who last changed the record (audit). */
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /** Skills live in the candidate_skills table; loaded only when accessed. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "candidate_skills", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "skill", nullable = false, length = 50)
    private Set<String> skills = new HashSet<>();

    @Version                                         // optimistic locking
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp                               // set by Hibernate, not the DB default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Candidate() {
        // required by JPA
    }

    public Candidate(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // ------------------------------------------------------------ behaviour

    /** Replace all skills, keeping the same collection instance (Hibernate tracks it). */
    public void replaceSkills(Set<String> newSkills) {
        this.skills.clear();
        if (newSkills != null) {
            this.skills.addAll(newSkills);
        }
    }

    // ------------------------------------------------------------ setters (editable fields only)

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setCurrentTitle(String currentTitle) {
        this.currentTitle = currentTitle;
    }

    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setYearsExperience(Integer yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public void setNoticePeriodDays(Integer noticePeriodDays) {
        this.noticePeriodDays = noticePeriodDays;
    }

    public void setOpenToWork(boolean openToWork) {
        this.openToWork = openToWork;
    }

    public void setCandidateStatus(CandidateStatus candidateStatus) {
        this.candidateStatus = candidateStatus;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}

