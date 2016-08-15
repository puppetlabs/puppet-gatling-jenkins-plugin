Puppet Gatling Jenkins Plugin
================================

A Jenkins plugin that post-processes gatling simulation data to generate useful reports for Puppet

## Setup

In this section of the readme, I will detail how to set up the plugin.

### Installation

First, you will need Maven to set up this plugin. If you are using Homebrew on OSX, it can be as easy as this:

    $ brew install maven

Next, make sure you are within the project repository so that you can install the required projects with maven.

    $ mvn install

This will download and install everything you need. (This might also take a while....) At the end, you should get a "Build Success" from maven like what you see below:

    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time: 8:25.535s
    [INFO] Finished at: Mon Jan 06 14:25:02 CST 2014
    [INFO] Final Memory: 32M/87M
    [INFO] ------------------------------------------------------------------------

With the success of the install, you can run a local development copy of Jenkins with maven by running:

    $ mvn hpi:run

Again, on the first run this may take some time to set up, but once it does it should deploy to your localhost...likely on port 8080. To visit Jenkins, you need to go to `localhost:8080/jenkins`. This is where you can set up the plugin for testing.

### Setup on Jenkins

### Creating a bunk job just to test the plugin via `mvnDebug hpi:run`

```
node {
    sh 'pwd'
    echo 'hi'
    sh 'rm -rf ./simulation-runner/results/*'
    sh 'sleep 1'
    sh 'cp -r /home/cprice/work/puppet-server/git/puppet-gatling-jenkins-plugin/src/test/resources/com/puppetlabs/jenkins/plugins/puppetgatling/steps/PuppetGatlingArchiverStepTest/workspace/* .'
    puppetGatlingArchive()
}
```

### Note On Development

If you wish to modify the code for this plugin, I highly suggest using IntelliJ IDEA Community Edition. It's a free IDE that plays really nice with the Jenkins plugin code. There are some gross tricks that you have to do if you want to use an IDE like Eclipse, where as with IntelliJ you do not. Both work, but I found that IntelliJ worked better for this project.

### Deployment to a real Jenkins Server

If you are ready to deploy to a _real_ jenkins server, and not the development one Maven gives you, you can package the plugin. This is done by running the command:

    $ mvn package

The resulting packaged plugin should be located in the _target_ directory in the root dir of the project. It should have something like _puppet-gatling-jenkins-plugin.jar_ and _puppet-gatling-jenkins-plugin.hpi_.

Now that we have the packaged plugin, we must upload it to the Jenkins server. To do this, click on the link "Manage Jenkins" on the left hand side of the Jenkins Dashboard. Then go to "Manage Plugins", and click the "Advanced" tab. There should be a heading called "Upload Plugin". Here is where you will upload the puppet-gatling-jenkins-plugin.hpi file we created. Once you do this, the plugin should be installed on the Jenkins server.

## Note About Release

After a couple of minor fixes that need to be made to the plugin, we are planning on doing a release to either Maven Central and or the Jenkins-CI wiki (...or other relevant plugin hosts). Once this has happened we will update this portion of the README for more information on how to obtain the latest build.

## Maintainence

Maintainers: Chris Price <chris@puppet.com>, Matthaus Owens <matthaus@puppet.com>, Ruth Linehan <ruth@puppet.com>

Tickets: https://tickets.puppetlabs.com/browse/PE. Set component = Puppet Server
