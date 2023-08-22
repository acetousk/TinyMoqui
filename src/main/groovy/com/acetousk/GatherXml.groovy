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

        //////////////// Data
        ResourceReference frameworkData = new UrlResourceReference().init("file:/home/user/coarchy/moqui/framework/data")
        List<ResourceReference> dataDirectories = frameworkData.getDirectoryEntries();
        ResourceReference baseComponentData = new UrlResourceReference().init("file:/home/user/coarchy/moqui/runtime/base-component")
        for (ResourceReference rr in baseComponentData.getDirectoryEntries()) {
            dataDirectories.addAll(rr.getChild("data").getDirectoryEntries() as List<ResourceReference>)
        }
        ResourceReference componentData = new UrlResourceReference().init("file:/home/user/coarchy/moqui/runtime/component")
        for (ResourceReference rr in componentData.getDirectoryEntries()) {
            dataDirectories.addAll(rr.getChild("data").getDirectoryEntries() as List<ResourceReference>)
        }

        byte[] array = new byte[7]; // length is bounded by 7
        new Random().nextBytes(array);
        String generatedString = new String(array, Charset.forName("UTF-8"));
        ResourceReference resourceResource = new UrlResourceReference().init("file:/home/user/play/TinyMoqui/src/main/resources")
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
        ResourceReference frameworkEntities = new UrlResourceReference().init("file:/home/user/coarchy/moqui/framework/entity")
        List<ResourceReference> entityDirectories = frameworkEntities.getDirectoryEntries();
        ResourceReference baseComponentEntities = new UrlResourceReference().init("file:/home/user/coarchy/moqui/runtime/base-component")
        for (ResourceReference rr in baseComponentEntities.getDirectoryEntries()) {
            entityDirectories.addAll(rr.getChild("entity").getDirectoryEntries() as List<ResourceReference>)
        }
        ResourceReference componentEntities = new UrlResourceReference().init("file:/home/user/coarchy/moqui/runtime/component")
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
