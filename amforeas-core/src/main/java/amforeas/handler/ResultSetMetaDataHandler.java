/**
 * Copyright (C) Alejandro Ayuso
 *
 * This file is part of Amforeas. Amforeas is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Amforeas is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Amforeas. If not, see <http://www.gnu.org/licenses/>.
 */

package amforeas.handler;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.dbutils.ResultSetHandler;
import amforeas.rest.xstream.Row;

/**
 * Converts a {@link java.sql.ResultSet} to a List of {@link amforeas.rest.xstream.Row}
 */
public class ResultSetMetaDataHandler implements ResultSetHandler<List<Row>> {

    @Override
    public List<Row> handle (ResultSet rs) throws SQLException {
        List<Row> results = new ArrayList<>();
        int rowId = 0;
        ResultSetMetaData metaData = rs.getMetaData();
        Map<String, Object> map = null;
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            map = new HashMap<String, Object>(2);
            map.put("tableName", metaData.getTableName(i));
            map.put("columnName", metaData.getColumnName(i));
            map.put("columnLabel", metaData.getColumnLabel(i));
            map.put("columnType", metaData.getColumnTypeName(i));
            map.put("columnSize", String.valueOf(metaData.getColumnDisplaySize(i)));
            map.put("precision", String.valueOf(metaData.getPrecision(i)));
            map.put("scale", String.valueOf(metaData.getScale(i)));

            // map.put("catalog_name", metaData.getCatalogName(i));
            // map.put("column_class_name", metaData.getColumnClassName(i));
            // map.put("schema_name", metaData.getSchemaName(i));
            // map.put("column_type", String.valueOf(metaData.getColumnType(i)));

            results.add(new Row(rowId++, map));
        }
        return results;
    }
}
