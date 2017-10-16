import urllib
import urllib2
import json
import psutil
import time

REST_API_PREFIX = "/api/v1"

def retrieve_token(serverURL, authToken):
    loginURL = serverURL + REST_API_PREFIX +"/security/login"
    body_value = {"authToken": authToken}
    body_value = urllib.urlencode(body_value)
    request = urllib2.Request(loginURL, body_value)
    result = urllib2.urlopen(request).read()
    jsonResult = json.loads(result)
    token = jsonResult["data"]["token"]

    return token;


def push_data(serverURL, container, access_token=None):
    if access_token is None:
        access_token = retrieve_token(serverURL)

    pushDataURL = serverURL + REST_API_PREFIX + "/topology/pushData"
    header_dict = {'Access-Token': access_token, 'Content-Type':'application/json'};
    body_value = json.dumps(container.__dict__)
    request = urllib2.Request(pushDataURL, headers=header_dict, data=body_value)
    result = urllib2.urlopen(request).read()
    jsonResult = json.loads(result)
    result = jsonResult["data"]
    return result


def cpu_utilization():
    return psutil.cpu_percent(interval=1)


def memory_utilization():
    return psutil.virtual_memory().percent


def disk_utilization():
    total = 0.0
    used = 0.0
    all_disk = psutil.disk_partitions()
    for disk in all_disk:
        try:
            disk_path = disk[1]
            disk_usage = psutil.disk_usage(disk_path)
            total += disk_usage[0]
            used += disk_usage[1]
        except:
            continue

    utilization = round(used/total, 2) * 100
    return utilization


def network_utilization():
    io_conters = psutil.net_io_counters()
    recvStart = io_conters.bytes_recv
    sentStart = io_conters.bytes_sent

    time.sleep(1)
    io_conters = psutil.net_io_counters()

    recvEnd = io_conters.bytes_recv - recvStart
    sentEnd = io_conters.bytes_sent - sentStart
    usage = 0.0
    usage += (recvEnd + sentEnd) * 8.0 / 1000 / 1000

    total = 0.0
    stats = psutil.net_if_stats()
    for i in stats:
        if "Interface" not in str(i):
            if stats[i][2] is not None:
                total += stats[i].speed
    return usage/total * 100


def system_platform():
    cpu_times = psutil.cpu_times()
    try:
        cpu_times.iowait
        return "Linux"
    except:
        return "Windows"


class Container:
    def __init__(self, agentName, startTime, endTime):
        self.startTime = startTime
        self.endTime = endTime
        self.agentName = agentName
        self.data = []

if __name__ == "__main__":
    # print 'Test Retrieve Token'
    # print retrieve_token("http://10.154.14.61:8080")

    print system_platform().iowait
