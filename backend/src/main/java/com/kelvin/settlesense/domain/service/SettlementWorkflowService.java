package com.kelvin.settlesense.domain.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kelvin.settlesense.domain.model.GroupMemberStatus;
import com.kelvin.settlesense.domain.model.GroupStatus;
import com.kelvin.settlesense.domain.model.LedgerSourceType;
import com.kelvin.settlesense.domain.model.Settlement;
import com.kelvin.settlesense.domain.model.SettlementStatus;
import com.kelvin.settlesense.domain.repository.ActivityEventRepository;
import com.kelvin.settlesense.domain.repository.GroupMemberRepository;
import com.kelvin.settlesense.domain.repository.GroupRepository;
import com.kelvin.settlesense.domain.repository.LedgerEntryRepository;
import com.kelvin.settlesense.domain.repository.SettlementRepository;

@Service
public class SettlementWorkflowService {

	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final SettlementRepository settlementRepository;
	private final LedgerEntryRepository ledgerEntryRepository;
	private final ActivityEventRepository activityEventRepository;
	private final LedgerService ledgerService;
	private final BalanceProjectionUpdater balanceProjectionUpdater;
	private final Clock clock;

	public SettlementWorkflowService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
			SettlementRepository settlementRepository, LedgerEntryRepository ledgerEntryRepository,
			ActivityEventRepository activityEventRepository, LedgerService ledgerService,
			BalanceProjectionUpdater balanceProjectionUpdater, Clock clock) {
		this.groupRepository = groupRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.settlementRepository = settlementRepository;
		this.ledgerEntryRepository = ledgerEntryRepository;
		this.activityEventRepository = activityEventRepository;
		this.ledgerService = ledgerService;
		this.balanceProjectionUpdater = balanceProjectionUpdater;
		this.clock = clock;
	}

	@Transactional
	public Settlement postSettlement(PostSettlementCommand command) {
		var now = clock.instant();
		var group = groupRepository.findById(command.groupId())
				.orElseThrow(() -> new IllegalArgumentException("group not found"));
		if (group.getStatus() != GroupStatus.ACTIVE) {
			throw new IllegalArgumentException("group must be active");
		}
		requireActiveMember(command.groupId(), command.fromUserId(), "settlement sender");
		requireActiveMember(command.groupId(), command.toUserId(), "settlement receiver");
		requireActiveMember(command.groupId(), command.createdByUserId(), "creator");

		var settlement = new Settlement();
		settlement.setGroupId(command.groupId());
		settlement.setFromUserId(command.fromUserId());
		settlement.setToUserId(command.toUserId());
		settlement.setCurrencyCode(MoneyRules.normalizeCurrencyCode(group.getCurrencyCode()));
		settlement.setAmountMinor(command.amountMinor());
		settlement.setSettlementDate(command.settlementDate());
		settlement.setStatus(SettlementStatus.POSTED);
		settlement.setCreatedByUserId(command.createdByUserId());
		settlement.setCreatedAt(now);
		settlement.setUpdatedAt(now);
		var savedSettlement = settlementRepository.save(settlement);

		ledgerEntryRepository.save(ledgerService.entryForSettlement(savedSettlement, now));
		activityEventRepository.save(ActivityEventFactory.event(group.getId(), command.createdByUserId(),
				"SETTLEMENT_POSTED", "SETTLEMENT", savedSettlement.getId(), "Settlement posted", "{}", now));
		balanceProjectionUpdater.refresh(group.getId(), group.getCurrencyCode());
		return savedSettlement;
	}

	@Transactional
	public Settlement cancelSettlement(Long settlementId, Long cancelledByUserId, String reason) {
		var now = clock.instant();
		var settlement = settlementRepository.findById(settlementId)
				.orElseThrow(() -> new IllegalArgumentException("settlement not found"));
		if (settlement.getStatus() == SettlementStatus.CANCELLED) {
			throw new IllegalArgumentException("settlement is already cancelled");
		}
		requireActiveMember(settlement.getGroupId(), cancelledByUserId, "cancelling user");

		settlement.setStatus(SettlementStatus.CANCELLED);
		settlement.setCancelledAt(now);
		settlement.setCancelledByUserId(cancelledByUserId);
		settlement.setCancellationReason(reason);
		settlement.setUpdatedAt(now);
		var savedSettlement = settlementRepository.save(settlement);

		var originalEntries = ledgerEntryRepository.findBySourceTypeAndSourceIdOrderByIdAsc(LedgerSourceType.SETTLEMENT,
				settlementId);
		if (!originalEntries.isEmpty()) {
			ledgerEntryRepository.saveAll(ledgerService.reversalEntries(originalEntries, settlementId, now));
		}
		activityEventRepository.save(ActivityEventFactory.event(settlement.getGroupId(), cancelledByUserId,
				"SETTLEMENT_CANCELLED", "SETTLEMENT", settlement.getId(), "Settlement cancelled", "{\"reason\":\""
						+ escapeJson(reason) + "\"}", now));
		balanceProjectionUpdater.refresh(settlement.getGroupId(), settlement.getCurrencyCode());
		return savedSettlement;
	}

	private void requireActiveMember(Long groupId, Long userId, String label) {
		if (!groupMemberRepository.existsByGroupIdAndUserIdAndStatus(groupId, userId, GroupMemberStatus.ACTIVE)) {
			throw new IllegalArgumentException(label + " must be an active group member");
		}
	}

	private String escapeJson(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
