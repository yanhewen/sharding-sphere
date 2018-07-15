/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.dbtest.env.authority;

import io.shardingsphere.core.constant.DatabaseType;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * Authority environment manager.
 *
 * @author panjuan
 */
@Slf4j
public final class AuthorityEnvironmentManager {
    
    private final Authority authority;
    
    private final Map<String, DataSource> instanceDataSourceMap;
    
    private final DatabaseType databaseType;
    
    public AuthorityEnvironmentManager(final String path, final Map<String, DataSource> instanceDataSourceMap, final DatabaseType databaseType) throws IOException, JAXBException {
        try (FileReader reader = new FileReader(path)) {
            authority = (Authority) JAXBContext.newInstance(Authority.class).createUnmarshaller().unmarshal(reader);
        }
        this.instanceDataSourceMap = instanceDataSourceMap;
        this.databaseType = databaseType;
    }
    
    /**
     * Initialize data.
     * 
     * @throws SQLException SQL exception
     */
    public void initialize() throws SQLException {
        Collection<String> initSQLs = authority.getInitSQLs(databaseType);
        if (initSQLs.isEmpty()) {
            return;
        }
        for (DataSource each : instanceDataSourceMap.values()) {
            executeOnInstanceDataSource(each, initSQLs);
        }
    }
    
    /**
     * Clean data.
     * 
     * @throws SQLException SQL exception
     */
    public void clean() throws SQLException {
        Collection<String> cleanSQLs = authority.getCleanSQLs(databaseType);
        if (cleanSQLs.isEmpty()) {
            return;
        }
        for (DataSource each : instanceDataSourceMap.values()) {
            executeOnInstanceDataSource(each, cleanSQLs);
        }
    }
    
    private void executeOnInstanceDataSource(final DataSource dataSource, final Collection<String> sqls) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            for (String each : sqls) {
                try {
                    connection.createStatement().execute(each);
                } catch (final SQLException ex) {
                    log.warn("Authority SQL: " + ex.getMessage());
                }
            }
        }
    }
}