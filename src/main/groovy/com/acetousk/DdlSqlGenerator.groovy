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
        String initialMoquiDirectory = "/home/user/coarchy/moqui"
        ResourceReference moquiRoot = new UrlResourceReference().init("file:" + initialMoquiDirectory)
        ResourceReference resourceResource = new UrlResourceReference().init("file:/home/user/play/TinyMoqui/src/main/resources")
        String generatedString = System.currentTimeMillis().toString()

        System.out.println("Start ${generatedString}")

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

        for (ResourceReference rr in entityDirectories) {
            MNode xmlFile = MNode.parse(rr);
            for (MNode child in xmlFile.children) {
//            MNode child = xmlFile.children.first()
                if (child.nodeName == "entity") {
                    StringBuilder sql = new StringBuilder("CREATE TABLE ")
                            .append(StringUtilities.camelCaseToPretty(child.attribute("entity-name")).toUpperCase().replace(" ", "_"))
                            .append(" (");
                    // Each field
                    List<String> primaryKeyFieldList = [];
                    for (MNode childChild in child.childList) {
                        if (childChild.nodeName == "field") {
                            String fieldName = StringUtilities.camelCaseToPretty(childChild.attribute("name")).toUpperCase().replace(" ", "_")
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
                        }

                        System.out.print("")
                    }
                    sql.append(" PRIMARY KEY (")
                    for (int i = 0; i < primaryKeyFieldList.size(); i++) {
                        if (i > 0) sql.append(", ")
                        sql.append(primaryKeyFieldList[i])
                    }
                    sql.append("));\n")

                    entityOutputSqlFile << sql
                    System.out.print("")
                }
            }
            System.out.println("Parsing File ${rr.fileName}")

        }

        //////////////// Setup H2
        Connection conn = null;
        Statement stmt = null;
        try {
            // STEP 1: Register JDBC driver
            Class.forName("org.h2.Driver");

            //STEP 2: Open a connection
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection("jdbc:h2:"+resourceResource+"/db/h2/aDb"+generatedString,"sa","");

            //STEP 3: Execute a query
            System.out.println("Creating table in given database...");
            stmt = conn.createStatement();

            for (String line in entityOutputSqlFile.getText().split("\n")) {
                stmt.executeUpdate(line);
            }
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

        System.out.println("Done with ${generatedString}")
    }
}
