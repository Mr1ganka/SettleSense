package com.kelvin.settlesense.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SystemControllerTests {

	@Test
	void statusReturnsServiceHealth() {
		var status = new SystemController().status();

		assertThat(status.service()).isEqualTo("settlesense");
		assertThat(status.status()).isEqualTo("ok");
		assertThat(status.timestamp()).isNotNull();
	}
}
