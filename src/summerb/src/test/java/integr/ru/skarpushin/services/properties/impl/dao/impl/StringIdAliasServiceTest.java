package integr.ru.skarpushin.services.properties.impl.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.annotation.SystemProfileValueSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.summerb.microservices.properties.internal.StringIdAliasService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-properties-app-context.xml")
@ProfileValueSourceConfiguration(SystemProfileValueSource.class)
@Transactional
public class StringIdAliasServiceTest {
	@Autowired
	private StringIdAliasService appAliasService;

	@BeforeTransaction
	public void verifyInitialDatabaseState() {
		// logic to verify the initial state before a transaction is started
	}

	@Before
	public void setUp() {
		// set up test data within the transaction
	}

	@Test
	@Rollback(false)
	public void testFindAlias_expectALiasWillBeFound() throws Exception {
		Map<String, Long> map = new HashMap<String, Long>();

		for (int i = 0; i < 10; i++) {
			String name = "any" + i;
			long alias = appAliasService.getAliasFor(name);
			assertTrue(alias > 0);
			map.put(name, alias);
		}

		for (int i = 0; i < 10; i++) {
			String name = "any" + i;
			long alias = appAliasService.getAliasFor(name);
			assertEquals(map.get(name).longValue(), alias);
		}
	}

}