package com.example.shortener;

import com.example.shortener.model.RandomKeyGen;
import com.example.shortener.services.UrlShortenerService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ShortenerApplicationTests {

    RandomKeyGen service = new RandomKeyGen();

	@Test
	public void test_key_length() {
        Assert.assertEquals(4, service.generateKey(4).length());
        Assert.assertEquals(5, service.generateKey(5).length());
	}

}
