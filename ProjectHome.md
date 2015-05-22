The work here is part of my PhD that I finished in 2010. It is presently sponsored by the EU LLP project [I-TUTOR](http://blog.unimc.it/i-tutor/).

Chatbots that a developed using AIML are based on static knowledge created by the developer.

This system has created the scripting language OwlLang, which enables injection of knowledge from ontologies (OWL-based) into the AIML. This has the benefit that static conversational trees can be generated for specific situations and the domain knowledge can then be changed with the aid of the ontologies.

The system is supplied with an example chatbot which is a survey tool. Two different ontologies are supplied. One which was used to survey students' view on the virtual learning environment Blackboard's module on programming at University of Reading. The other was used for a survey on a research website.

To get a chatbot running you have to:

Checkout the source code (Downloads to follow)

Install mysql
  * Create a ProgramD user with password
  * Change DATABASE CONFIGURATION in ProgramD/core.xml to match the database password and user name

within mysql console create DATABASE and use: e.g. jdbc:mysql:///aimlchat
  * Create DATABASE aimlchat
  * Use DATABASE aimlchat

Run scripts from ProgramD/resources/database or run each line consecutively from files within the mysql console to create the necessary tables
  * db-chatlog.script
  * db-multiplexor.script

Download and install Eclipse from http://www.eclipse.org/downloads/ - I usually use the Java EE Developers version
  * Open Eclipse using the folder where you checked out ProgramD as workspace
  * In the dropdown of the run option you should have several run scripts available


Run simple-console or similar
  * Start the survey chat by saying "CONNECT" to the bot



To run the simple Ajax server.
  * Set port in conf/ajaxJetty.xml to an available port
  * Go to org.aitools.programd.server.jettyinterface.AjaxD and change the private String serverURL to the name of the machine with port. E.g my machine is schemerhom and I use port 8081 which was free.
  * Run the run-script Ajax
  * In a browser type "http://schermerhom:8081/chat/Chatter" (change with your own settings)
Now the chatbot should be running in the browser


To run the bot using a JabberServer
  * Have access to a Jabber server (e.g. Openfire)
  * in conf/listeners.xml change the JabberListener from enabled="false" to enabled="true"
  * in conf/listeners.xml change the settings to your user settings