package com.example.corebanking.repository;

import com.example.corebanking.domain.LedgerEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
}
