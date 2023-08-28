package com.acetousk

// Moqui
import org.moqui.resource.ResourceReference
import org.moqui.resource.UrlResourceReference
import org.moqui.util.MNode
import org.moqui.util.SystemBinding
import org.moqui.util.StringUtilities
import java.nio.charset.Charset

// Sql
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement

// Java
import java.time.Instant;

class DdlSqlGenerator {

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
                    //            List<MNode> seedDataChildren = [];
                    //            List<MNode> masterChildren = [];

                    for (MNode childChild in child.childList) {
                        if (childChild.nodeName == "field") fieldChildren.push(childChild);
                        else if (childChild.nodeName == "index") indexChildren.push(childChild);
                        else if (childChild.nodeName == "relationship") relationshipChildren.push(childChild);
                    }

                    String entityName = StringUtilities.camelCaseToPretty(child.attribute("entity-name")).toUpperCase().replace(" ", "_")

                    // See createTables in EntityDbMeta.groovy
                    StringBuilder sql = new StringBuilder("CREATE TABLE ")
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

                    // See createIndexes in EntityDbMeta.groovy
                    for (MNode indexNode in indexChildren) {
                        // See createIndexes in EntityDbMeta.groovy
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

                        System.out.print("")
                    }

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
            stmt.executeUpdate(entityOutputSqlFile.getText());
            System.out.println("Created table in given database...");

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
                if(conn!=null) conn.close();
            } catch(SQLException se){
                se.printStackTrace();
            } //end finally try
        } //end try

        System.out.println("Done with ${databaseUrl}")
    }
}
