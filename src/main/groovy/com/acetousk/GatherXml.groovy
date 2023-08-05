package com.acetousk

import org.moqui.resource.ResourceReference
import org.moqui.resource.UrlResourceReference
import org.moqui.util.MClassLoader
import org.moqui.util.MNode

class GatherXml {

    static MClassLoader moquiClassLoader

    String getGreeting() {
        return 'Hello World!'
    }

    static void main(String[] args) {

        ClassLoader pcl = (Thread.currentThread().getContextClassLoader() ?: this.class.classLoader) ?: System.classLoader
        moquiClassLoader = new MClassLoader(pcl)

        System.out.println("Start")

        ResourceReference frameworkData = new UrlResourceReference().init("file:/home/user/coarchy/moqui/framework/data")
        System.println("${frameworkData.getDirectoryEntries()}")

        URL defaultConfUrl = new URL("file:/home/user/coarchy/moqui/framework/data/CommonL10nData.xml")
        if (defaultConfUrl == null) throw new IllegalArgumentException("Could not find MoquiDefaultConf.xml file on the classpath")
        MNode newConfigXmlRoot = MNode.parse(defaultConfUrl.toString(), defaultConfUrl.newInputStream());

//        System.out.println("newConfigXmlRoot ${newConfigXmlRoot}")
        System.out.println("Done")
    }
}