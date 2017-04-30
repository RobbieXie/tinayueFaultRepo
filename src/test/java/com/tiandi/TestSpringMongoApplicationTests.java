package com.tiandi;

import com.tiandi.service.FaultInjectionInfoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestSpringMongoApplicationTests {

	@Autowired
	private FaultInjectionInfoService fs;

	@Test
	public void contextLoads() {
		Object a = fs.generateTestCase("COM-COM-F");
	}

}
