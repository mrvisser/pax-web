package org.sakaiproject.oae.testpersist;

import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Service
@Property(name="alias", value="/api/test-persistence")
public class PersistenceServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private final static Logger LOGGER = LoggerFactory.getLogger(PersistenceServlet.class);

  @Activate
  public void activate() {
    LOGGER.info("Initializing...");
    Cluster cluster = getOrCreateCluster();
    
    if (cluster.describeKeyspace("OAE") == null) {
      ColumnFamilyDefinition cfd = HFactory.createColumnFamilyDefinition("OAE", "StringContent", ComparatorType.UTF8TYPE);
      KeyspaceDefinition kd = HFactory.createKeyspaceDefinition("OAE", ThriftKsDef.DEF_STRATEGY_CLASS, 1, Arrays.asList(cfd));
      getOrCreateCluster().addKeyspace(kd, true);
    }
    
    LOGGER.info("Finished building keyspace.");
  }
  
  /**
   * {@inheritDoc}
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String tenantId = req.getParameter("tenant");
    
    StringSerializer ss = new StringSerializer();
    Keyspace ks = HFactory.createVirtualKeyspace("OAE", tenantId, new StringSerializer(), getOrCreateCluster());
    HFactory.createColumnQuery(ks, ss, ss, ss).setColumnFamily("StringContent");
    
    CqlQuery<String, String, String> query = new CqlQuery<String, String, String>(ks, ss, ss, ss);
    query.setQuery("select * from StringContent");
    QueryResult<CqlRows<String,String,String>> result = query.execute();
    
    resp.getWriter().append("[");
    for (Row<String, String, String> r : result.get().getList()) {
      resp.getWriter().append("{");
      for (HColumn<String, String> c : r.getColumnSlice().getColumns()) {
        resp.getWriter().append(String.format("\"%s\": \"%s\",", c.getName(), c.getValue()));
      }
      resp.getWriter().append("},");
    }
    resp.getWriter().append(",]");
    
  }

  /**
   * {@inheritDoc}
   * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String tenantId = req.getParameter("tenant");
    String columnName = req.getParameter("column");
    String value = req.getParameter("value");

    Keyspace ks = HFactory.createVirtualKeyspace("OAE", tenantId, new StringSerializer(), getOrCreateCluster());
    Mutator<String> m = HFactory.createMutator(ks, new StringSerializer());
    m.addInsertion(UUID.randomUUID().toString(), "StringContent", HFactory.createStringColumn(columnName, value));
    m.execute();
  }

  private Cluster getOrCreateCluster() {
    return HFactory.getOrCreateCluster("Test Persistence", "127.0.0.1:9160");
  }
}
