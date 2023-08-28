package com.acetousk

import org.moqui.resource.ResourceReference
import org.moqui.resource.UrlResourceReference
import org.moqui.util.MNode
import org.moqui.util.StringUtilities

import java.nio.charset.Charset

class GatherXml {

    static void recurseOutputXml(MNode child, File dataOutputFile, depth = 1) {
        for (int i = 0; i < depth; i++) dataOutputFile << "\t";
        dataOutputFile << "<" + child.nodeName;
        child.attributeMap.forEach((key,value) -> {
            dataOutputFile << ' ' + key + '="' + StringUtilities.encodeForXmlAttribute(value) + '"';
        })
        if (child.childList == null) {
            dataOutputFile << "/>\n"
        } else {
            dataOutputFile << ">\n"
            for (MNode childChild in child.childList) {
                recurseOutputXml(childChild, dataOutputFile, depth + 1)
            }
            for (int i = 0; i < depth; i++) dataOutputFile << "\t";
            dataOutputFile << "</" + child.nodeName + ">\n";
            System.out.print('');
        }
    }

    static void main(String[] args) {

        System.out.println("Start")

        String initialMoquiDirectory = "/home/user/play/moqui"
        ResourceReference moquiRoot = new UrlResourceReference().init("file:" + initialMoquiDirectory)
        ResourceReference resourceResource = new UrlResourceReference().init("file:/home/user/play/TinyMoqui/src/main/resources")
        String generatedString = System.currentTimeMillis().toString()

        //////////////// Data
        ResourceReference frameworkData = moquiRoot.findChildDirectory("framework/data")
        List<ResourceReference> dataDirectories = frameworkData.getDirectoryEntries();
        ResourceReference baseComponentData = moquiRoot.findChildDirectory("runtime/base-component")
        for (ResourceReference rr in baseComponentData.getDirectoryEntries()) {
            dataDirectories.addAll(rr.getChild("data").getDirectoryEntries() as List<ResourceReference>)
        }
        ResourceReference componentData = moquiRoot.findChildDirectory("runtime/component")
        for (ResourceReference rr in componentData.getDirectoryEntries()) {
            dataDirectories.addAll(rr.getChild("data").getDirectoryEntries() as List<ResourceReference>)
        }

        ResourceReference dataOutputResource = resourceResource.makeFile("aData"+generatedString+".xml")
        File dataOutputFile = dataOutputResource.getFile()
        dataOutputFile << "<entity-facade-xml>\n"

        for (ResourceReference rr in dataDirectories) {
            MNode xmlFile = MNode.parse(rr);
            for (MNode child in xmlFile.children){
                recurseOutputXml(child, dataOutputFile)
            }
            System.out.println("Parsing File ${rr.fileName}")
        }

        dataOutputFile << "</entity-facade-xml>"

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

        ResourceReference entityOutputResource = resourceResource.makeFile("aEntity"+generatedString+".xml")
        File entityOutputFile = entityOutputResource.getFile()
        entityOutputFile << "<entities xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://moqui.org/xsd/entity-definition-3.xsd\">\n"

        for (ResourceReference rr in entityDirectories) {
            MNode xmlFile = MNode.parse(rr);
            for (MNode child in xmlFile.children)
                entityOutputFile << "\t" + child.toString() + "\n"
            System.out.println("Parsing File ${rr.fileName}")
        }

        entityOutputFile << "</entities>"

//        System.out.println("newConfigXmlRoot ${newConfigXmlRoot}")
        System.out.println("Done")
    }
}
