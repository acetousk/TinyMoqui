package com.acetousk

import org.moqui.resource.ResourceReference

// Moqui

import org.moqui.resource.UrlResourceReference
import org.moqui.util.MNode
import org.moqui.util.StringUtilities

import java.sql.Connection

// Sql

import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement

// Java

class SqlGenerator {

    static void main(String[] args) {

        //////////////// Initial Setup
        String initialMoquiDirectory = "/home/user/play/moqui"
        ResourceReference moquiRoot = new UrlResourceReference().init("file:" + initialMoquiDirectory)
        ResourceReference resourceResource = new UrlResourceReference().init("file:/home/user/play/TinyMoqui/src/main/resources")
        String generatedString = System.currentTimeMillis().toString()

        //////////////// Entity
        ResourceReference frameworkEntities = moquiRoot.findChildDirectory("framework/entity")
        List<ResourceReference> entityDirectories = frameworkEntities.getDirectoryEntries();
        ResourceReference baseComponentEntities = moquiRoot.findChildDirectory("runtime/base-component")
        for (ResourceReference rr in baseComponentEntities.getDirectoryEntries()) {
            entityDirectories.addAll(rr.getChild("entity").getDirectoryEntries() as List<ResourceReference>)
        }
        ResourceReference componentEntities = moquiRoot.findChildDirectory("runtime/component")
        for (ResourceReference rr in componentEntities.getDirectoryEntries()) {
            entityDirectories.addAll(rr.getChild("entity").getDirectoryEntries() as List<ResourceReference>)
        }

        ResourceReference entityOutputSqlResource = resourceResource.makeFile("aDdlSql"+generatedString+".sql")
        File entityOutputSqlFile = entityOutputSqlResource.getFile()

        System.out.println("Start ${entityOutputSqlResource.fileName}")

        for (ResourceReference rr in entityDirectories) {
            MNode xmlFile = MNode.parse(rr);

            for (MNode child in xmlFile.children) {

                if (child.nodeName == "entity") {
                    // Need to go through all the entires once and sort them into field, index, relationship, seed-data, master
                    List<MNode> fieldChildren = [];
                    List<MNode> indexChildren = [];
                    List<MNode> relationshipChildren = [];
                    List<MNode> seedDataChildren = [];
                    List<MNode> masterChildren = [];

                    for (MNode childChild in child.childList) {
                        if (childChild.nodeName == "field") fieldChildren.push(childChild);
                        else if (childChild.nodeName == "index") indexChildren.push(childChild);
                        else if (childChild.nodeName == "relationship") relationshipChildren.push(childChild);
                        else if (childChild.nodeName == "seed-data") seedDataChildren.push(childChild);
                        else if (childChild.nodeName == "master") masterChildren.push(childChild);
                    }

                    String entityName = StringUtilities.camelCaseToPretty(child.attribute("entity-name")).toUpperCase().replace(" ", "_")

                    // See createTables in EntityDbMeta.groovy
                    StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                            .append(entityName)
                            .append(" (");

                    // Each field
                    List<String> primaryKeyFieldList = [];
                    for (MNode childChild in fieldChildren) {
                        String fieldName = StringUtilities.camelCaseToPretty(childChild.attribute("name")).toUpperCase().replace(" ", "_")
                        if (fieldName == "VALUE") fieldName = "THE_VALUE"
                        if (fieldName == "CONDITION") fieldName = "THE_CONDITION"
                        sql.append(fieldName).append(" ")
                        if (childChild.attribute("type") == "id") sql.append("VARCHAR(40)")
                        else if (childChild.attribute("type") == "id-long") sql.append("VARCHAR(255)")
                        else if (childChild.attribute("type") == "date") sql.append("DATE")
                        else if (childChild.attribute("type") == "time") sql.append("TIME")
                        else if (childChild.attribute("type") == "date-time") sql.append("TIMESTAMP")
                        else if (childChild.attribute("type") == "number-integer") sql.append("NUMERIC(20,0)")
                        else if (childChild.attribute("type") == "number-decimal") sql.append("NUMERIC(26,6)")
                        else if (childChild.attribute("type") == "number-float") sql.append("DOUBLE")
                        else if (childChild.attribute("type") == "currency-amount") sql.append("NUMERIC(24,4)")
                        else if (childChild.attribute("type") == "currency-precise") sql.append("NUMERIC(25,5)")
                        else if (childChild.attribute("type") == "text-indicator") sql.append("CHAR(1)")
                        else if (childChild.attribute("type") == "text-short") sql.append("VARCHAR(63)")
                        else if (childChild.attribute("type") == "text-medium") sql.append("VARCHAR(255)")
                        else if (childChild.attribute("type") == "text-intermediate") sql.append("VARCHAR(1023)")
                        else if (childChild.attribute("type") == "text-long") sql.append("VARCHAR(4095)")
                        else if (childChild.attribute("type") == "text-very-long") sql.append("CLOB")
                        else if (childChild.attribute("type") == "binary-very-long") sql.append("BLOB")
                        if (childChild.attribute("is-pk") == "true") {
                            primaryKeyFieldList.push(fieldName)
                            sql.append(" NOT NULL")
                        }
                        sql.append(", ")
                        System.out.print("")
                    }
                    sql.append(" PRIMARY KEY (")
                    for (int i = 0; i < primaryKeyFieldList.size(); i++) {
                        if (i > 0) sql.append(", ")
                        sql.append(primaryKeyFieldList[i])
                    }
                    sql.append("));\n")

                    /////////////// create explicit and foreign key auto indexes

                    // See createIndexes in EntityDbMeta.groovy
                    for (MNode indexNode in indexChildren) {
                        sql.append("CREATE ")
                        if (indexNode.attribute("unique") == "true") sql.append("UNIQUE ")
                        sql.append("INDEX ")
                        sql.append(indexNode.attribute("name")).append(" ON ").append(entityName)

                        sql.append(" (")
                        boolean isFirst = true
                        for (MNode indexFieldNode in indexNode.children("index-field")) {
                            if (isFirst) isFirst = false else sql.append(", ");
                            sql.append(StringUtilities.camelCaseToPretty(indexFieldNode.attribute("name")).toUpperCase().replace(" ", "_"))

                            System.out.print("")
                        }
                        sql.append(");\n")
                    }

                    // See later in createIndexes in EntityDbMeta.groovy
/*                    for (MNode indexNode in relationshipChildren) {
                        // see makeFkIndexName in EntityDbMeta.groovy
                        System.out.print("")

                        List<MNode> indexNodeKeyMapList = [];
                        for (MNode relationshipChildrenChild in indexNode.childList) {
                            if (relationshipChildrenChild.nodeName == "key-map") indexNodeKeyMapList.push(relationshipChildrenChild);
                        }
                        if (indexNodeKeyMapList.size() > 0) {
                            String relatedEntityName = indexNode.attribute("related").tokenize(".").last()
                            StringBuilder indexName = new StringBuilder()
                            if (indexNode.attribute("fk-name")) indexName.append(indexNode.attribute("fk-name"))
                            if (!indexName) {
                                String title = indexNode.attribute("fk-name") ?: ""
                                String edEntityName = child.attribute("entity-name")
                                int edEntityNameLength = edEntityName.length()

                                int commonChars = 0
                                while (title.length() > commonChars && edEntityNameLength > commonChars &&
                                        title.charAt(commonChars) == edEntityName.charAt(commonChars)) commonChars++

                                int relLength = relatedEntityName.length()
                                int relEndCommonChars = relatedEntityName.length() - 1
                                while (relEndCommonChars > 0 && edEntityNameLength > relEndCommonChars &&
                                        relatedEntityName.charAt(relEndCommonChars) == edEntityName.charAt(edEntityNameLength - (relLength - relEndCommonChars)))
                                    relEndCommonChars--

                                if (commonChars > 0) {
                                    indexName.append(edEntityName)
                                    for (char cc in title.substring(0, commonChars).chars) if (Character.isUpperCase(cc)) indexName.append(cc)
                                    indexName.append(title.substring(commonChars))
                                    indexName.append(relatedEntityName.substring(0, relEndCommonChars + 1))
                                    if (relEndCommonChars < (relLength - 1)) for (char cc in relatedEntityName.substring(relEndCommonChars + 1).chars)
                                        if (Character.isUpperCase(cc)) indexName.append(cc)
                                } else {
                                    indexName.append(edEntityName).append(title)
                                    indexName.append(relatedEntityName.substring(0, relEndCommonChars + 1))
                                    if (relEndCommonChars < (relLength - 1)) for (char cc in relatedEntityName.substring(relEndCommonChars + 1).chars)
                                        if (Character.isUpperCase(cc)) indexName.append(cc)
                                }

                                // logger.warn("Index for entity [${ed.getFullEntityName()}], title=${title}, commonChars=${commonChars}, indexName=${indexName}")
                                // logger.warn("Index for entity [${ed.getFullEntityName()}], relatedEntityName=${relatedEntityName}, relEndCommonChars=${relEndCommonChars}, indexName=${indexName}")
                            }
                            // see constraint-name-clip-length
                            int maxLength = 60-3
                            if (indexName.length() > maxLength) {
                                // remove vowels from end toward beginning
                                for (int i = indexName.length()-1; i >= 0 && indexName.length() > maxLength; i--) {
                                    if ("AEIOUaeiou".contains(indexName.charAt(i) as String)) indexName.deleteCharAt(i)
                                }
                                // clip
                                if (indexName.length() > maxLength) {
                                    indexName.delete(maxLength-1, indexName.length())
                                }
                            }
                            indexName.insert(0, "IDX");

                            // See later in createIndexes in EntityDbMeta.groovy
                            sql.append("CREATE INDEX ")
                            sql.append(indexName).append(" ON ").append(entityName)

                            // TODO: Allow for self closing relationships that don't have a key-map or modify the xml files to contain a key-map when it's implicit
                            sql.append(" (")
                            boolean isFirst = true
                            int count = 0
                            for (MNode indexNodeKeyMap in indexNodeKeyMapList) {
                                System.out.print("")
                                if (isFirst) isFirst = false else sql.append(", ")

                                String fieldName = StringUtilities.camelCaseToPretty(indexNodeKeyMap.attribute("field-name")).toUpperCase().replace(" ", "_")
                                sql.append(fieldName)
                                count ++;
                            }

                            if (count == 0) {
                                System.out.print("")
                            }
                            sql.append(");\n")
                        }
                    }*/
                    /////////////// create foreign keys to all other tables that exist


                    // See createExtended in EntityValueImpl.java
                    /*
                    for (MNode seedDataNode in seedDataChildren) {


                        for (MNode entityNode in seedDataNode.children) {

                            StringBuilder createSql = new StringBuilder(500);
                            createSql.append("INSERT INTO ").append(StringUtilities.camelCaseToPretty(entityNode.nodeName.tokenize(".").last()).toUpperCase().replace(" ", "_")).append(" (");

                            int size = entityNode.attributeMap.size();
                            StringBuilder values = new StringBuilder(size*3);

                            boolean isFirst = true
                            entityNode.attributeMap.each{ key, value ->
                                if (isFirst) {
                                    isFirst = false
                                } else {
                                    sql.append(", ");
                                    values.append(", ");
                                }

                                sql.append(StringUtilities.camelCaseToPretty(key).toUpperCase().replace(" ", "_"));
                                values.append("?");
                            }

                            sql.append(") VALUES (").append(values.toString()).append(")");

                            System.out.print("")
                        }

                    } */

                    entityOutputSqlFile << sql
                }

            }

            System.out.println("Parsing File ${rr.fileName}")

        }

        System.out.println("Done with ${entityOutputSqlResource.fileName}")

        String databaseUrl = resourceResource.getLocation()-"file:"+"/db/h2/aDb"+generatedString
        System.out.println("Start ${databaseUrl}")

        //////////////// Setup H2
        Connection conn = null;
        Statement stmt = null;
        try {
            // STEP 1: Register JDBC driver
            Class.forName("org.h2.Driver");

            //STEP 2: Open a connection
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection("jdbc:h2:"+databaseUrl,"sa","");

            //STEP 3: Execute a query
            System.out.println("Creating table in given database...");
            stmt = conn.createStatement();

//            for (String line in entityOutputSqlFile.getText().split("\n")) {
//                stmt.executeUpdate(line);
//            }
            int rowCount = stmt.executeUpdate(entityOutputSqlFile.getText());
            System.out.println("Created ${rowCount} table in given database...");

            // STEP 4: Clean-up environment
            stmt.close();
            conn.close();
        } catch(SQLException se) {
            //Handle errors for JDBC
            se.printStackTrace();
        } catch(Exception e) {
            //Handle errors for Class.forName
            e.printStackTrace();
        } finally {
            //finally block used to close resources
            try{
                if(stmt!=null) stmt.close();
            } catch(SQLException se2) {
            } // nothing we can do
            try {
                if(conn!=null) {
                    conn.close();
                }
            } catch(SQLException se){
                se.printStackTrace();
            } //end finally try
        } //end try



        System.out.println("Done with ${databaseUrl}")
    }
}
