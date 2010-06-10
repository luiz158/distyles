package com.bank.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import com.bank.domain.InsufficientFundsException;
import com.bank.domain.TransferConfirmation;

/**
 * System tests for {@link TransferService}.
 */
@ContextConfiguration("/com/bank/app/application-config.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class TransferServiceTests {
	
	JdbcTemplate jdbcTemplate;
	
	@Autowired
	TransferService transferService;
	
	@Autowired
	void init(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}
	
	@Test
	public void transferServiceIsTransactional() {
		assertTrue("transferService is not an aop proxy", AopUtils.isAopProxy(transferService));

		boolean isTxProxy = false;
		for (Advisor advisor : ((Advised)transferService).getAdvisors()) {
			if (TransactionInterceptor.class.equals(advisor.getAdvice().getClass())) {
				isTxProxy = true;
			}
		}

		assertTrue("transferService does not have transactional advice applied", isTxProxy);
	}
	
	@Test
	public void transfer10Dollars() throws InsufficientFundsException {
		assertThat(queryForBalance("A123"), equalTo(100.00));
		final double transferAmount = 10.00;
		TransferConfirmation conf = transferService.transfer(transferAmount, "A123", "C456");
		double transferTotal =  transferAmount + conf.getFeeAmount();
		assertThat(queryForBalance("A123"), equalTo(100.00 - transferTotal));
	}
	
	private double queryForBalance(String acctNumber) {
		return jdbcTemplate.queryForObject("select balance from account where id = ?", Double.class, acctNumber);
	}
}