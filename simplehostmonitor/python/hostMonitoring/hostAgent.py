import util
import socket
import sys
import time
import datetime

class Container:
    def __init__(self, agentName, startTime, endTime):
        self.startTime = startTime
        self.endTime = endTime
        self.agentName = agentName
        self.data = []


class Host:
    def __init__(self, hostName):
        self.typeName = "Host"
        self.properties = {"hostId":hostName, "name":hostName}

DURATION = 60

print "Please enter the server url, for example: http://10.1.1.1:8080"

prompt = ">"
url = raw_input(prompt)

print "The url is " + url

print "Please enter the auth token"

prompt = ">"
authToken = raw_input(prompt)

print "Try to retrieve token from the url..."

access_token = util.retrieve_token(url, authToken)

if access_token is not None:
    print "Retrieve access token successfully."
else:
    print "Failed to retrieve access token, please check log for detail."
    try:
        sys.exit(0)
    except:
        pass

hostName = socket.gethostname()
while True:
    host = Host(hostName)
    hostProperty = host.properties.copy()

    cpu = {};
    cpu["utilization"] = util.cpu_utilization()
    cpu["host"] = hostProperty

    memory = {}
    memory["utilization"] = util.memory_utilization()
    memory["host"] = hostProperty

    disk = {};
    disk["diskUtilization"] = util.disk_utilization()
    disk["host"] = hostProperty

    network = {};
    network["utilization"] = util.network_utilization()
    network["host"] = hostProperty

    os = {}
    os["host"] = hostProperty
    os["type"] = util.system_platform()

    host.properties["cpus"] = cpu
    host.properties["memory"] = memory
    host.properties["network"] = network
    host.properties["storage"] = disk
    host.properties["os"] = os

    end = int(round(time.time() * 1000))
    start = end - (DURATION * 1000)
    container = Container("Rest_Agent_"+hostName, start, end)
    container.data.append(host.__dict__)


    pushDataResult = util.push_data(url, container, access_token)

    print 'Push data at %s, result is %s' % (datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S'), pushDataResult)
    time.sleep(DURATION)




