# Renders a figure, see https://diagrams.mingrammer.com/

from diagrams import *
from diagrams.onprem.network import *
from diagrams.onprem.compute import *
from diagrams.onprem.queue import *
from diagrams.onprem.database import *
from diagrams.programming.language import *
from diagrams.generic.storage import *

graph_attr = {
    "margin":"-2, -2"
}

with Diagram("Distributed Systems are Asynchronous", graph_attr=graph_attr, show=False, outformat="png"):
    lb = Envoy("Ingress")
    wildfly = Wildfly("Java EE App Server")
    kafka = Kafka("Kakfa streams")
    mq = Activemq("ActiveMQ")
    faas = Nodejs("Functions")
    quarkus1 = Java("Quarkus App #1")
    quarkus2 = Java("Quarkus App #2")
    vertx = Java("Vert.x App")
    pg = Postgresql("Postgresql DB")
    mdb = Mariadb("Maria DB")
    neo = Neo4J("Graph DB")
    fs = Storage("Repository")

    lb >> [quarkus1, vertx, faas]
    quarkus1 >> [quarkus2, vertx]
    vertx >> neo
    quarkus2 >> pg    
    faas >> kafka
    kafka - vertx
    mq - wildfly
    wildfly - fs
    vertx >> wildfly
    quarkus2 - mq
    quarkus1 - kafka
    wildfly >> mdb