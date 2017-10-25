import java.text.SimpleDateFormat;

ts = server.TopologyService;
ds = server.DataService;

def countLimit=50
def logException = true
org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog("Q2.groovy");

SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
currentTime = df.format(new Date());

serverType = ts.getType("VMWESXServer");

serverSet = ts.getObjectsOfType(serverType)

def output = new StringBuilder();

output.append("Current Date").append(", ").append("vCenter Name").append(", ").append("Cluster name").append(", ").append("ESX Host").append(", ").append("Health Status").append(", ").append("# of CPU").append(", ").append("CPU Utilization").append(", ").append("CPU Allocated(MHz)").append(", ").append("Memory Utilization").append(", ").append("Memory Capacity(GB)").append(", ").append("Memory Consumed(GB)").append(", ").append("NICs").append(", ").append("Version").append(", ").append("HBAs").append(", ").append("# of VM configured").append(", ").append("# of VM Running").append(", ").append("Datastore Throughput(MB)").append(", ").append("Datastore Commands Issued/Sec").append(", ").append("Net Mb/Sec").append(", ").append("Network Packets Sent/Sec").append(", ").append("Network Packet Received/Sec").append("\n")


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

		serverCpus = server?.cpus?.hostCPUs;
		serverMemory = server?.memory;
		if(serverCpus == null || serverMemory == null){
			continue
		}

		serverCPUUsed = ds.retrieveLatestValue(serverCpus, "usedHz")?.getValue()?.getAvg();
		def serverCPUUtilization = ds.retrieveLatestValue(serverCpus, "utilization")?.getValue()?.getAvg();
		if (serverCPUUtilization != null) {
			serverCPUUtilization = autoScale(serverCPUUtilization, 10, 0, 1) + '%';
		}
		def serverCpuAllocated = ds.retrieveLatestValue(serverCpus, "totalHz")?.getValue()?.getAvg();
		if (serverCpuAllocated != null) {
			serverCpuAllocated = autoScale(serverCpuAllocated, 10, 6, 1);
		}

		def serverMemCapacity = ds.retrieveLatestValue(serverMemory, "capacity")?.getValue()?.getAvg();
		if (serverMemCapacity != null) {
			serverMemCapacity = new BigDecimal(serverMemCapacity/Math.pow(1024,1)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
		}

		def serverMemUsed = ds.retrieveLatestValue(serverMemory, "consumed")?.getValue()?.getAvg();
		if (serverMemUsed != null) {
			serverMemUsed = autoScale(serverMemUsed, 1024, 2, 1);
		}
		def serverMemUtilization = ds.retrieveLatestValue(serverMemory?.hostMemory, "utilization")?.getValue()?.getAvg();
		if (serverMemUtilization != null) {
			serverMemUtilization = autoScale(serverMemUtilization, 1024, 0, 1) + '%';
		}

		def serverNICs = server?.network?.interfaces?.size();
		def serverVersion = server?.esxVersion;
		def serverHBAs = ds.retrieveLatestValue(server, "hbaCount")?.getValue()?.getAvg();
		if (serverHBAs != null) {
			serverHBAs = autoScale(serverHBAs, 1, 0, 0);
		}

		def totalVMs = ds.retrieveLatestValue(server, "virtualMachinesCount")?.getValue()?.getAvg();
		def poweredOnVMs = ds.retrieveLatestValue(server, "virtualMachinesPoweredOnCount")?.getValue()?.getAvg();

		serverStorage = server?.storage;
		def datastoreThroughtput = ds.retrieveLatestValue(serverStorage, "datastoreTransferRate")?.getValue()?.getAvg();
		if (datastoreThroughtput != null) {
			datastoreThroughtput = autoScale(datastoreThroughtput, 1024, 1, 1);
		}
		def datastoreIOPs = ds.retrieveLatestValue(serverStorage, "datastoreIops")?.getValue()?.getAvg();
		if (datastoreIOPs != null) {
			datastoreIOPs = autoScale(datastoreIOPs, 1, 1, 1);
		}

		serverNetwork = server?.network?.hostNetwork;
		def networkTransferRate = ds.retrieveLatestValue(serverNetwork, "transferRate")?.getValue()?.getAvg();
		if (networkTransferRate != null) {
			networkTransferRate = autoScale(networkTransferRate, 1024, 2, 1);
		}
		def networkPacketsSent = ds.retrieveLatestValue(serverNetwork, "packetsSent")?.getValue()?.getAvg();
		if (networkPacketsSent != null) {
			networkPacketsSent = autoScale(networkPacketsSent, 1, 1, 1);
		}
		def networkPacketsReceive = ds.retrieveLatestValue(serverNetwork, "packetsReceived")?.getValue()?.getAvg();
		if (networkPacketsReceive != null) {
			networkPacketsReceive = autoScale(networkPacketsReceive, 1, 1, 1);
		}

		def outputMid = new StringBuilder();
		outputMid.append(currentTime).append(", ").append(vcName).append(", ").append(clusterName).append(", ").append(serverName).append(", ").append(healthStatus).append(", ").append(serverCpusCount).append(", ").append(serverCPUUtilization).append(", ").append(serverCpuAllocated).append(", ").append(serverMemUtilization).append(", ").append(serverMemCapacity).append(", ").append(serverMemUsed).append(", ").append(serverNICs).append(", ").append(serverVersion).append(", ").append(serverHBAs).append(", ").append(totalVMs).append(", ").append(poweredOnVMs).append(", ").append(datastoreThroughtput).append(", ").append(datastoreIOPs).append(", ").append(networkTransferRate).append(", ").append(networkPacketsSent).append(", ").append(networkPacketsReceive).append("\n");

		output.append(outputMid.toString());

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