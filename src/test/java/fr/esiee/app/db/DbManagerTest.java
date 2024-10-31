package fr.esiee.app.db;

import fr.esiee.app.db.entities.Chat;
import fr.esiee.app.db.entities.LLM;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbExecute;
import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.DbTransaction;
import io.helidon.http.NotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class DbManagerTest {


  @Mock
  private DbClient dbClient;

  private DbManager dbManager;

  @BeforeEach
  void initializeDBManger() throws Exception {
    try (var mocked = MockitoAnnotations.openMocks(this)) {
      var config = Config.global().get("db");
      dbClient = DbClient.builder(config).build();
      dbManager = new DbManager(dbClient);
      dbManager.setupSchema();
      dbManager.setupData();
    }
  }

  @Test
  void testSetupSchema() {
    var tableName = dbClient.execute().createQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES;").execute().map(e -> e.column("TABLE_NAME").getString()).toList();
    assertTrue(tableName.contains("CHAT"));
    assertTrue(tableName.contains("LLM"));
    assertTrue(tableName.contains("PROMPT"));
  }

  @Test
  void testSetupData() throws IOException {
    var llms = dbClient.execute()
            .namedQuery("select-all-llms")
            .map(e -> e.as(LLM.class))
            .toList();
    System.out.println(llms);
  }


}
