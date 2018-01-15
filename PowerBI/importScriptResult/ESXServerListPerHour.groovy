import java.text.SimpleDateFormat;
import com.quest.nitro.service.sl.interfaces.data.ObservationQuery.RetrievalType;

ts = server.TopologyService;
ds = server.DataService;

def countLimit=50
def logException = true
org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog("ESXServerListPerHour.groovy");

currentDate = new Date();

endTime = currentDate.getTime()

Calendar c = Calendar.getInstance();
c.setTime(currentDate);
c.add(Calendar.DAY_OF_MONTH, -1);

startTime = c.getTime().getTime();

serverType = ts.getType("VMWESXServer");

serverSet = ts.getObjectsOfType(serverType)

def output = new StringBuilder();

output.append("Start Time").append(",").append("End Time").append(", ").append("vCenter Name").append(", ").append("Cluster name").append(", ").append("ESX Host").append(", ").append("Health Status").append(", ").append("# of CPU").append(", ").append("CPU Utilization").append(", ").append("CPU Allocated(MHz)").append(", ").append("Memory Utilization").append(", ").append("Memory Capacity(GB)").append(", ").append("Memory Consumed(GB)").append(", ").append("NICs").append(", ").append("Version").append(", ").append("HBAs").append(", ").append("# of VM configured").append(", ").append("# of VM Running").append(", ").append("Datastore Throughput(MB)").append(", ").append("Datastore Commands Issued/Sec").append(", ").append("Net Mb/Sec").append(", ").append("Network Packets Sent/Sec").append(", ").append("Network Packet Received/Sec").append("\n")

metricsToObjects = getEsxHostMetricsToObjMap();
def result = batchQueryVmMetrics();

def count=0
for (server in serverSet) {
	if(count >= countLimit){
		continue
	}
	try{
		vcName = server.virtualCenter?.name;
		def clusterName="";
		esxParent = server.parent;
		if (esxParent && esxParent?.topologyTypeName == "VMWCluster") {
			clusterName = esxParent?.name;
		}
		serverName = server.name;
		healthStatus = server.aggregateState;
		serverCpusCount = server?.cpus?.cores;

		serverCpus = server?.get("cpus/hostCPUs");
		serverMemory = server?.get("memory");
		serverHostMem = server?.get("memory/hostMemory");
		def serverNICs = server?.network?.interfaces?.size();
		def serverVersion = server?.esxVersion;
		serverStorage = server?.get("storage");
		serverNetwork = server?.get("network/hostNetwork");
		if(serverCpus == null || serverMemory == null){
			continue
		}
		
		for(int i = 0; i < 24; i++) {
		   
		    def start = result.getValues(server, "virtualMachinesCount")?.get(i).getStartTime();
			def end = result.getValues(server, "virtualMachinesCount")?.get(i).getEndTime()

		    serverCPUUsed = result.getValues(serverCpus, "usedHz")?.get(i)?.getValue()?.getAvg();
		    def serverCPUUtilization = result.getValues(serverCpus, "utilization")?.get(i)?.getValue()?.getAvg();
		    if (serverCPUUtilization != null && !serverCPUUtilization.isNaN()) {
			    serverCPUUtilization = autoScale(serverCPUUtilization, 10, 0, 1) + '%';
		    }
		    def serverCpuAllocated = result.getValues(serverCpus, "totalHz")?.get(i)?.getValue()?.getAvg();
		    if (serverCpuAllocated != null && !serverCpuAllocated.isNaN()) {
			    serverCpuAllocated = autoScale(serverCpuAllocated, 10, 6, 1);
		    }

		    def serverMemCapacity = result.getValues(serverMemory, "capacity")?.get(i)?.getValue()?.getAvg();
		    if (serverMemCapacity != null && !serverMemCapacity.isNaN()) {
			    serverMemCapacity = new BigDecimal(serverMemCapacity/Math.pow(1024,1)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
		    }

		    def serverMemUsed = result.getValues(serverMemory, "consumed")?.get(i)?.getValue()?.getAvg();
		    if (serverMemUsed != null && !serverMemUsed.isNaN()) {
			    serverMemUsed = autoScale(serverMemUsed, 1024, 2, 1);
		    }
		    def serverMemUtilization = result.getValues(serverHostMem, "utilization")?.get(i)?.getValue()?.getAvg();
		    if (serverMemUtilization != null && !serverMemUtilization.isNaN()) {
			    serverMemUtilization = autoScale(serverMemUtilization, 1024, 0, 1) + '%';
		    }

		
		    def serverHBAs = result.getValues(server, "hbaCount")?.get(i)?.getValue()?.getAvg();
		    if (serverHBAs != null && !serverHBAs.isNaN()) {
			    serverHBAs = autoScale(serverHBAs, 1, 0, 0);
		    }

		    def totalVMs = result.getValues(server, "virtualMachinesCount")?.get(i)?.getValue()?.getAvg();
		    def poweredOnVMs = result.getValues(server, "virtualMachinesPoweredOnCount")?.get(i)?.getValue()?.getAvg();


		    def datastoreThroughtput = result.getValues(serverStorage, "datastoreTransferRate")?.get(i)?.getValue()?.getAvg();
		    if (datastoreThroughtput != null && !datastoreThroughtput.isNaN()) {
			    datastoreThroughtput = autoScale(datastoreThroughtput, 1024, 1, 1);
		    }
		    def datastoreIOPs = result.getValues(serverStorage, "datastoreIops")?.get(i)?.getValue()?.getAvg();
		    if (datastoreIOPs != null && !datastoreIOPs.isNaN()) {
			    datastoreIOPs = autoScale(datastoreIOPs, 1, 1, 1);
		    }

		    def networkTransferRate = result.getValues(serverNetwork, "transferRate")?.get(i)?.getValue()?.getAvg();
		    if (networkTransferRate != null && !networkTransferRate.isNaN()) {
			    networkTransferRate = autoScale(networkTransferRate, 1024, 2, 1);
		    }
		    def networkPacketsSent = result.getValues(serverNetwork, "packetsSent")?.get(i)?.getValue()?.getAvg();
		    if (networkPacketsSent != null && !networkPacketsSent.isNaN()) {
			    networkPacketsSent = autoScale(networkPacketsSent, 1, 1, 1);
		    }
		    def networkPacketsReceive = result.getValues(serverNetwork, "packetsReceived")?.get(i)?.getValue()?.getAvg();
		    if (networkPacketsReceive != null && !networkPacketsReceive.isNaN()) {
			    networkPacketsReceive = autoScale(networkPacketsReceive, 1, 1, 1);
		    }

		    def outputMid = new StringBuilder();
		    outputMid.append(start).append(",").append(end).append(", ").append(vcName).append(", ").append(clusterName).append(", ").append(serverName).append(", ").append(healthStatus).append(", ").append(serverCpusCount).append(", ").append(serverCPUUtilization).append(", ").append(serverCpuAllocated).append(", ").append(serverMemUtilization).append(", ").append(serverMemCapacity).append(", ").append(serverMemUsed).append(", ").append(serverNICs).append(", ").append(serverVersion).append(", ").append(serverHBAs).append(", ").append(totalVMs).append(", ").append(poweredOnVMs).append(", ").append(datastoreThroughtput).append(", ").append(datastoreIOPs).append(", ").append(networkTransferRate).append(", ").append(networkPacketsSent).append(", ").append(networkPacketsReceive).append("\n");

		    output.append(outputMid.toString());
        }
		count++
	}catch(Exception e){
		if(logException){
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			output.append(errors.toString()).append("\n")
		}
	}
}
log.info("powerbi: "+output.toString())
return output.toString();

//-------------------
def autoScale(value, factor, pow, scale){
	return new BigDecimal(value/Math.pow(factor,pow)).setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
}

def addObjToSet(obj, objSet) {
    if (obj) {
	    objSet.add(obj);
	}
}

def getEsxHostMetricsToObjMap() {
    def metricsToObjects = [:];
	def serverCpuSet = [] as Set;
	def serverMemSet = [] as Set;
	def serverHostMemSet = [] as Set;
	def serverStorageSet = [] as Set;
	def serverNetworkSet = [] as Set;
    try{        
	    metricsToObjects.put("hbaCount", serverSet);
		metricsToObjects.put("virtualMachinesCount", serverSet);
		metricsToObjects.put("virtualMachinesPoweredOnCount", serverSet);
        for (server in serverSet) {	  
            addObjToSet(server?.get("cpus/hostCPUs"), serverCpuSet);
	        addObjToSet(server?.get("memory"), serverMemSet);
            addObjToSet(server?.get("memory/hostMemory"), serverHostMemSet);
            addObjToSet(server?.get("storage"), serverStorageSet);
			addObjToSet(server?.get("network/hostNetwork"), serverNetworkSet);
        }
		metricsToObjects.put("usedHz", serverCpuSet);
		metricsToObjects.put("totalHz", serverCpuSet);
		metricsToObjects.put("utilization", serverCpuSet);
		metricsToObjects.put("capacity", serverMemSet);
		metricsToObjects.put("consumed", serverMemSet);
		metricsToObjects.put("utilization", serverHostMemSet);
		metricsToObjects.put("datastoreTransferRate", serverStorageSet);
		metricsToObjects.put("datastoreIops", serverStorageSet);
        metricsToObjects.put("transferRate", serverNetworkSet);		
		metricsToObjects.put("packetsSent", serverNetworkSet);	
		metricsToObjects.put("packetsReceived", serverNetworkSet);	
	}catch(Exception e){
		if(logException){
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
		}
	}
	return metricsToObjects;
}

def batchQueryVmMetrics() {    
    query = ds.createObservationQuery();
	query.setStartTime(startTime);
	query.setEndTime(endTime);
	query.setRetrievalType(RetrievalType.RAW);
	query.setGranularity(3600*1000);
	query.setNumberOfValues(24);
	for (mto in metricsToObjects.entrySet()) {
	    query.include(mto.getValue(), mto.getKey());
	}		
	return ds.performQuery(query);
}
