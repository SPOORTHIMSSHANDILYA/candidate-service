-- =====================================================================
-- Seed data: 25,000 realistic dummy candidates (+ ~90,000 skill rows)
-- For local development / performance testing only. NOT a Flyway migration.
--
-- Run in MySQL Workbench against the candidate_management database
-- (the app must have started once, so Flyway has created the tables).
-- Takes a few seconds. Re-runnable: it deletes previous seed rows first.
--
-- * Names, cities and titles are realistic; emails use reserved example domains
--   (example.com / .org / .net), so no real person's address is used.
-- * Values are deterministic (CRC32 of the row number), so every run gives the same data.
-- * Seed rows are marked by created_by = one of the five seed recruiters below,
--   which is also how cleanup_seed_candidates.sql finds them.
-- =====================================================================

SET SESSION cte_max_recursion_depth = 30000;
SET SQL_SAFE_UPDATES = 0;   -- Workbench's safe-update mode would block the DELETE below (created_by is not a key)
SET @rows := 25000;

-- Remove a previous seed run (skills go too: ON DELETE CASCADE)
DELETE FROM candidates
WHERE created_by IN ('anita.k', 'rahul.m', 'sara.j', 'vikram.p', 'meena.s');

-- ---------------------------------------------------------------------
-- 1. Numbers 1..25000
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS seed_numbers;
CREATE TEMPORARY TABLE seed_numbers (n INT PRIMARY KEY);
INSERT INTO seed_numbers (n)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < @rows)
SELECT n FROM seq;

-- ---------------------------------------------------------------------
-- 2. One row per candidate, with "dice rolls" per attribute
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS seed_rolls;
CREATE TEMPORARY TABLE seed_rolls AS
SELECT n,
       CRC32(CONCAT('first', n))   AS r_first,
       CRC32(CONCAT('last', n))    AS r_last,
       CRC32(CONCAT('family', n))  AS r_family,
       CRC32(CONCAT('exp', n))     AS r_exp,
       CRC32(CONCAT('city', n))    AS r_city,
       CRC32(CONCAT('company', n)) AS r_company,
       CRC32(CONCAT('notice', n))  AS r_notice,
       CRC32(CONCAT('status', n))  AS r_status,
       CRC32(CONCAT('open', n))    AS r_open,
       CRC32(CONCAT('phone', n))   AS r_phone,
       CRC32(CONCAT('domain', n))  AS r_domain,
       CRC32(CONCAT('created', n)) AS r_created,
       CRC32(CONCAT('updated', n)) AS r_updated,
       CRC32(CONCAT('version', n)) AS r_version,
       CRC32(CONCAT('owner', n))   AS r_owner
FROM seed_numbers;

-- ---------------------------------------------------------------------
-- 3. Turn the rolls into realistic values
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS seed_candidates;
CREATE TEMPORARY TABLE seed_candidates AS
SELECT
    n,
    ELT(1 + r_first % 60,
        'Aarav','Vivaan','Aditya','Arjun','Rohan','Karthik','Rahul','Vikram','Siddharth','Nikhil',
        'Ankit','Varun','Pranav','Harsh','Manish','Suresh','Ravi','Kiran','Abhishek','Gaurav',
        'Ananya','Priya','Sneha','Kavya','Divya','Pooja','Neha','Isha','Riya','Tanvi',
        'Meera','Shreya','Aishwarya','Lakshmi','Nandini','Swathi','Deepika','Anjali','Pallavi','Bhavna',
        'Mohammed','Imran','Fatima','Ayesha','Joseph','Thomas','Maria','Sarah','Daniel','David',
        'Emily','James','Wei','Mei','Hiroshi','Sofia','Lucas','Olivia','Noah','Amara') AS first_name,
    ELT(1 + r_last % 50,
        'Sharma','Verma','Gupta','Iyer','Nair','Reddy','Rao','Patel','Shah','Mehta',
        'Joshi','Kulkarni','Desai','Pillai','Menon','Krishnan','Subramanian','Chopra','Malhotra','Kapoor',
        'Singh','Kumar','Das','Banerjee','Chatterjee','Mukherjee','Bose','Ghosh','Agarwal','Bansal',
        'Khan','Ahmed','Hussain','Fernandes','DSouza','Thomas','George','Varghese','Mathew','Jacob',
        'Smith','Johnson','Brown','Wilson','Taylor','Chen','Wang','Tanaka','Garcia','Muller') AS last_name,
    1 + r_family % 9 AS family,
    -- experience: mostly 0-12 years, fewer seniors up to 25
    CASE WHEN r_exp % 10 < 8 THEN r_exp % 13 ELSE 8 + r_exp % 18 END AS years_experience,
    1 + r_city % 20 AS city_no,
    ELT(1 + r_company % 30,
        'Infosys','TCS','Wipro','HCLTech','Tech Mahindra','Accenture','Cognizant','Capgemini','Deloitte','IBM',
        'Flipkart','Swiggy','Zomato','Razorpay','PhonePe','Paytm','Freshworks','Zoho','CRED','Meesho',
        'Amazon','Microsoft','Google','Oracle','SAP Labs','Adobe','Salesforce','Atlassian','Thoughtworks','Walmart Global Tech') AS current_company,
    ELT(1 + r_notice % 10, 0, 15, 30, 30, 30, 60, 60, 90, 90, 45) AS notice_period_days,
    CASE WHEN r_status % 100 < 80 THEN 'ACTIVE'
         WHEN r_status % 100 < 95 THEN 'INACTIVE'
         ELSE 'ARCHIVED' END AS candidate_status,
    (r_open % 100 < 72) AS open_to_work,
    r_phone, r_domain, r_created, r_updated, r_version, r_owner
FROM seed_rolls;

-- Families of job titles (also drive the skills below)
--  1 Backend Java   2 Frontend   3 Full stack   4 QA          5 DevOps/Cloud
--  6 Data           7 Mobile     8 Product/PM   9 Python backend

-- ---------------------------------------------------------------------
-- 4. Insert the candidates
-- ---------------------------------------------------------------------
INSERT INTO candidates (first_name, last_name, email, phone, current_title, current_company,
                        city, country, years_experience, notice_period_days, open_to_work,
                        candidate_status, created_by, updated_by, version, created_at, updated_at)
SELECT
    s.first_name,
    s.last_name,
    LOWER(CONCAT(REPLACE(s.first_name, ' ', ''), '.', REPLACE(s.last_name, ' ', ''), '.', s.n, '@',
                 ELT(1 + s.r_domain % 4, 'example.com', 'example.org', 'example.net', 'mail.example.com'))),
    -- phone: country code + 10 digits
    CONCAT(ELT(s.city_no, '+91','+91','+91','+91','+91','+91','+91','+91','+91','+91',
                          '+91','+91','+91','+91','+65','+971','+44','+1','+49','+61'),
           ' ', 6 + s.r_phone % 4, LPAD(s.r_phone % 1000000000, 9, '0')),
    -- title: level by experience + family
    CONCAT(CASE WHEN s.years_experience < 2 THEN 'Junior '
                WHEN s.years_experience >= 12 THEN 'Lead '
                WHEN s.years_experience >= 7 THEN 'Senior '
                ELSE '' END,
           ELT(s.family, 'Java Developer', 'Frontend Developer', 'Full Stack Developer', 'QA Engineer',
                         'DevOps Engineer', 'Data Engineer', 'Android Developer', 'Product Manager',
                         'Python Developer')),
    s.current_company,
    ELT(s.city_no, 'Bengaluru','Bengaluru','Bengaluru','Hyderabad','Hyderabad','Pune','Pune','Chennai','Mumbai','Delhi',
                   'Gurugram','Noida','Kochi','Ahmedabad','Singapore','Dubai','London','Toronto','Berlin','Sydney'),
    ELT(s.city_no, 'India','India','India','India','India','India','India','India','India','India',
                   'India','India','India','India','Singapore','United Arab Emirates','United Kingdom','Canada','Germany','Australia'),
    s.years_experience,
    s.notice_period_days,
    s.open_to_work,
    s.candidate_status,
    ELT(1 + s.r_owner % 5, 'anita.k', 'rahul.m', 'sara.j', 'vikram.p', 'meena.s'),
    CASE WHEN s.r_version % 4 = 0 THEN NULL ELSE ELT(1 + s.r_owner % 5, 'anita.k', 'rahul.m', 'sara.j', 'vikram.p', 'meena.s') END,
    s.r_version % 4,
    -- created within the last 2 years
    -- (UTC_TIMESTAMP is used directly, not via a @variable: MySQL stores a date in a
    --  @variable as text in the server's default charset, which can clash with utf8mb4)
    UTC_TIMESTAMP(6) - INTERVAL (s.r_created % (730 * 24 * 60)) MINUTE,
    -- updated sometime after created, never in the future
    LEAST(UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) - INTERVAL (s.r_created % (730 * 24 * 60)) MINUTE
                                            + INTERVAL (s.r_updated % (180 * 24 * 60)) MINUTE)
FROM seed_candidates s;

-- ---------------------------------------------------------------------
-- 5. Skills: 3-6 per candidate, matching their job family
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS seed_family_skills;
CREATE TEMPORARY TABLE seed_family_skills (family INT, is_core BOOLEAN, skill VARCHAR(50));
INSERT INTO seed_family_skills (family, is_core, skill) VALUES
 (1,1,'java'),(1,1,'spring boot'),(1,0,'microservices'),(1,0,'mysql'),(1,0,'kafka'),(1,0,'aws'),(1,0,'docker'),(1,0,'hibernate'),
 (2,1,'javascript'),(2,1,'react'),(2,0,'typescript'),(2,0,'html'),(2,0,'css'),(2,0,'redux'),(2,0,'next.js'),(2,0,'angular'),
 (3,1,'javascript'),(3,1,'node.js'),(3,0,'react'),(3,0,'typescript'),(3,0,'mongodb'),(3,0,'java'),(3,0,'aws'),(3,0,'postgresql'),
 (4,1,'manual testing'),(4,1,'selenium'),(4,0,'java'),(4,0,'testng'),(4,0,'api testing'),(4,0,'postman'),(4,0,'cypress'),(4,0,'jmeter'),
 (5,1,'aws'),(5,1,'docker'),(5,0,'kubernetes'),(5,0,'terraform'),(5,0,'jenkins'),(5,0,'linux'),(5,0,'azure'),(5,0,'ansible'),
 (6,1,'python'),(6,1,'sql'),(6,0,'spark'),(6,0,'airflow'),(6,0,'aws'),(6,0,'snowflake'),(6,0,'kafka'),(6,0,'power bi'),
 (7,1,'kotlin'),(7,1,'android'),(7,0,'java'),(7,0,'jetpack compose'),(7,0,'firebase'),(7,0,'rest api'),(7,0,'flutter'),(7,0,'git'),
 (8,1,'product management'),(8,1,'agile'),(8,0,'jira'),(8,0,'scrum'),(8,0,'roadmapping'),(8,0,'sql'),(8,0,'stakeholder management'),(8,0,'analytics'),
 (9,1,'python'),(9,1,'django'),(9,0,'fastapi'),(9,0,'postgresql'),(9,0,'docker'),(9,0,'redis'),(9,0,'aws'),(9,0,'celery');

INSERT INTO candidate_skills (candidate_id, skill)
SELECT c.id, fs.skill
FROM seed_candidates s
JOIN candidates c          ON c.email = LOWER(CONCAT(REPLACE(s.first_name, ' ', ''), '.', REPLACE(s.last_name, ' ', ''), '.', s.n, '@',
                                         ELT(1 + s.r_domain % 4, 'example.com', 'example.org', 'example.net', 'mail.example.com')))
JOIN seed_family_skills fs ON fs.family = s.family
WHERE fs.is_core = 1                                         -- every candidate has the 2 core skills
   OR CRC32(CONCAT(s.n, fs.skill)) % 100 < 35;               -- plus ~35% of the others

-- ---------------------------------------------------------------------
-- 6. Refresh index statistics so MySQL picks good query plans
-- ---------------------------------------------------------------------
ANALYZE TABLE candidates, candidate_skills;

DROP TEMPORARY TABLE IF EXISTS seed_numbers, seed_rolls, seed_candidates, seed_family_skills;
SET SQL_SAFE_UPDATES = 1;   -- back to Workbench's default

-- Summary
SELECT COUNT(*) AS seeded_candidates FROM candidates
WHERE created_by IN ('anita.k', 'rahul.m', 'sara.j', 'vikram.p', 'meena.s');
SELECT candidate_status, COUNT(*) AS candidates FROM candidates GROUP BY candidate_status;
SELECT COUNT(*) AS skill_rows FROM candidate_skills;
