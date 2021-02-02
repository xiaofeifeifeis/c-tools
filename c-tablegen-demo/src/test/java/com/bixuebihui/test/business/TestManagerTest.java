package com.bixuebihui.test.business;

import com.bixuebihui.datasource.BitmechanicDataSource;
import com.bixuebihui.dbcon.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class TestManagerTest {

    @Bean
    DataSource getDataSource(){
        BitmechanicDataSource res = new BitmechanicDataSource();
        DatabaseConfig cfg =new DatabaseConfig();
        cfg.setClassName("com.mysql.jdbc.cf.Driver");
        cfg.setDburl("jdbc:mysql://127.0.0.1:3306/test");
        cfg.setUsername("test");
        cfg.setPassword("test123");
        res.setDatabaseConfig(cfg);
        return res;
    }

    @Autowired
    TestManager man;

    @Test
    public void testManInsert(){
        //man.ins
    }

}
