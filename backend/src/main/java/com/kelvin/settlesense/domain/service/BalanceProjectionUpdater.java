package com.kelvin.settlesense.domain.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kelvin.settlesense.domain.repository.BalanceProjectionRepository;
import com.kelvin.settlesense.domain.repository.LedgerEntryRepository;

@Service
public class BalanceProjectionUpdater {

	private final LedgerEntryRepository ledgerEntryRepository;
	private final BalanceProjectionRepository balanceProjectionRepository;
	private final BalanceProjectionService balanceProjectionService;
	private final Clock clock;

	public BalanceProjectionUpdater(LedgerEntryRepository ledgerEntryRepository,
			BalanceProjectionRepository balanceProjectionRepository, BalanceProjectionService balanceProjectionService,
			Clock clock) {
		this.ledgerEntryRepository = ledgerEntryRepository;
		this.balanceProjectionRepository = balanceProjectionRepository;
		this.balanceProjectionService = balanceProjectionService;
		this.clock = clock;
	}

	@Transactional
	public void refresh(Long groupId, String currencyCode) {
		var entries = ledgerEntryRepository.findByGroupIdOrderByIdAsc(groupId);
		var projections = balanceProjectionService.rebuild(groupId, currencyCode, entries, clock);
		balanceProjectionRepository.deleteByGroupId(groupId);
		balanceProjectionRepository.flush();
		balanceProjectionRepository.saveAll(projections);
	}
}
