package com.svims.repository;

import com.svims.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Vendor entity
 */
@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByEmail(String email);
    Optional<Vendor> findByGstin(String gstin);
    List<Vendor> findByStatus(Vendor.VendorStatus status);
    
    @Query("SELECT v FROM Vendor v WHERE v.riskScore > :threshold ORDER BY v.riskScore DESC")
    List<Vendor> findHighRiskVendors(Double threshold);
}

