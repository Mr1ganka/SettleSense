package com.kelvin.settlesense.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kelvin.settlesense.domain.model.LedgerEntry;
import com.kelvin.settlesense.domain.model.LedgerSourceType;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

	List<LedgerEntry> findByGroupIdOrderByIdAsc(Long groupId);

	List<LedgerEntry> findBySourceTypeAndSourceIdOrderByIdAsc(LedgerSourceType sourceType, Long sourceId);
}
