import os, time
import socket
import urllib2
import operator
import copy

try:
    import json
except:
    import simplejson as json

import xmltodict

import sys
reload(sys)  # Reload does the trick!
sys.setdefaultencoding('UTF8')


old_jnlp_fp = open("../eulermind/target/jnlp/launch.jnlp", "rb")

old_jnlp = xmltodict.parse(old_jnlp_fp)


def create_new_jnlp(jnlpname, codebase, islib):
    if islib:
        filename = jnlpname + "-lib" + ".jnlp"
    else:
        filename = jnlpname + ".jnlp"

    new_jnlp = copy.deepcopy(old_jnlp);
    new_jnlp["jnlp"]["@spec"] = "6.0+"
    new_jnlp["jnlp"]["@codebase"] = codebase
    new_jnlp["jnlp"]["@href"] = filename

    information = new_jnlp["jnlp"]["information"]
    information["vendor"] = "eulermind"
    information["homepage"] = {"@href" : "../index.html"}
    information["description"] = {"@kind" : "short", "#text" : "a infinite mindmap tool"}
    information["icon"] = {"@href" : "eulermind.png"}
    information["shortcut"] = {"@online" : "false", "@install" : "false"}
    information["shortcut"]["desktop"] = { }
    information["shortcut"]["menu"] = { }

    new_jnlp["jnlp"]["resources"]["j2se"] = {"@version" : "1.7+",  "@href" : "http://www.oracle.com/technetwork/java/javase/downloads/index.html"}
    new_jnlp["jnlp"]["update"] = { "@check" : "timeout",  "@policy" : "prompt-update"}

    jars = new_jnlp["jnlp"]["resources"]["jar"]

    new_jars = []
    if islib:
        for jar in jars:
            if jar["@href"] != "eulermind.jar":
                new_jars.append(jar)

        new_jnlp["jnlp"]["resources"]["jar"] = new_jars

        new_jnlp["jnlp"].pop("application-desc", None)
        new_jnlp["jnlp"]["component-desc"] = { }
    else:
        for jar in jars:
            if jar["@href"] == "eulermind.jar":
                new_jars.append(jar)

        new_jnlp["jnlp"]["resources"]["jar"] = new_jars

        new_jnlp["jnlp"]["resources"]["extension"] = { "@name" : jnlpname+"-lib", "@href" : jnlpname+"-lib"+".jnlp" }

    new_jnlp_str = xmltodict.unparse(new_jnlp, pretty=True)
    new_jnlp_fp = open("../eulermind/target/jnlp/" + filename, "wb");
    new_jnlp_fp.write(new_jnlp_str)

def create_new_jnlp_and_lib(jnlpname, codebase):
    create_new_jnlp(jnlpname, codebase, True)
    create_new_jnlp(jnlpname, codebase, False)

create_new_jnlp_and_lib("localhost", "http://localhost/jnlp")
create_new_jnlp_and_lib("home", "http://192.168.1.104/jnlp")
create_new_jnlp_and_lib("eulermind", "http://www.eulermind.com/jnlp")
create_new_jnlp_and_lib("ali", "http://123.57.204.59/jnlp")
create_new_jnlp_and_lib("github", "https://raw.githubusercontent.com/ninesunqian/ninesunqian.github.io/master/jnlp/")
