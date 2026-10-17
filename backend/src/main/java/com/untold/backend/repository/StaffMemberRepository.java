package com.untold.backend.repository;

import com.untold.backend.model.StaffMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {
    
    @Query("SELECT COUNT(s) FROM StaffMember s WHERE s.status = 'Active' OR s.status = 'Standby'")
    long countOnDutyStaff();

    @Query("SELECT COUNT(DISTINCT s.zone) FROM StaffMember s WHERE s.status = 'Active'")
    long countActiveZones();

    java.util.Optional<com.untold.backend.model.StaffMember> findByUserId(Long userId);
}
