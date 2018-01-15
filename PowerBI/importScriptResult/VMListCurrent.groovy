import java.text.SimpleDateFormat;

ts = server.TopologyService;
ds = server.DataService;

def countLimit=50
def logException = false
org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog("VMListCurrent.groovy");

SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
currentTime = df.format(new Date());

vmType = ts.getType("VMWVirtualMachine");

vmSet = ts.getObjectsOfType(vmType)

def output = new StringBuilder();

output.append("Current Date").append("\t").append("vCenter Name").append("\t").append("cluster name").append("\t").append("esxhost ").append("\t").append("vm name").append("\t").append("VM CPU allocated(MHz)").append("\t").append("VM CPU used(MHz)").append("\t").append("VM memory allocated(GB)").append("\t").append("VM memory used(GB)").append("\t").append("datastore name").append("\t").append("VM ds allocated(GB)").append("\t").append("VM ds used(GB)").append("\n")


def count=0
for (vm in vmSet) {
	if(count >= countLimit){
		continue
	}
	try{
		isTemplate = vm.isTemplate;
		if(isTemplate) {
			continue;
		}
		
		vcName = vm.virtualCenter?.name;
		esxName = vm.esxServer?.name;
		def clusterName="";
		esxParent = vm.esxServer?.parent;
		if (esxParent && esxParent.topologyTypeName == "VMWCluster") {
			clusterName = esxParent?.name;
		}
		vmName = vm.name;
		
		vmCpus = vm?.cpus?.hostCPUs;
		vmMemory = vm?.memory;
		if(vmCpus == null || vmMemory == null){
			continue
		}
		
		vmCPUUsed = ds.retrieveLatestValue(vm, "usedHz")?.getValue()?.getAvg();
		vmCPUUtilization = ds.retrieveLatestValue(vmCpus, "utilization")?.getValue()?.getAvg();
		def vmCpuAllocated;
		
		if (vmCPUUsed != null) {
			vmCPUUsed =  new BigDecimal(vmCPUUsed/Math.pow(1000,2)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();		
			vmCpuAllocated = new BigDecimal(vmCPUUsed*100/vmCPUUtilization).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
		}
		
		vmMemAllocated = ds.retrieveLatestValue(vmMemory, "allocated")?.getValue()?.getAvg();
		vmMemUsed = ds.retrieveLatestValue(vmMemory, "consumed")?.getValue()?.getAvg();
		if (vmMemAllocated != null) {
			vmMemAllocated = new BigDecimal(vmMemAllocated/1024).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
		}
		
		if (vmMemUsed != null) {
			vmMemUsed = new BigDecimal(vmMemUsed/Math.pow(1024,2)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
		}

		//output.append(vcName).append("\t").append(clusterName).append("\t").append(esxName).append("\t").append(vmName).append("\t").append(vmCpuAllocated).append("\t").append(vmCPUUsed).append("\t").append(vmMemAllocated).append("\t").append(vmMemUsed).append("\t");
		dsUsageList = vm.storage?.datastoreUsage;
		def outputMid = new StringBuilder();
		outputMid.append(currentTime).append("\t").append(vcName).append("\t").append(clusterName).append("\t").append(esxName).append("\t").append(vmName).append("\t").append(vmCpuAllocated).append("\t").append(vmCPUUsed).append("\t").append(vmMemAllocated).append("\t").append(vmMemUsed).append("\t");
		
		for (dsUsage in dsUsageList) {
			def outputds = new StringBuilder(outputMid.toString());
		
			dsName = dsUsage.datastore?.name;
			committed = ds.retrieveLatestValue(dsUsage, "committed")?.getValue()?.getAvg();
			uncommitted = ds.retrieveLatestValue(dsUsage, "uncommitted")?.getValue()?.getAvg();		
			allocatedSpace  = committed + uncommitted; 
			if (committed != null) {
				committed = committed/Math.pow(1024,3);
			}
			if (allocatedSpace != null) {
				allocatedSpace = allocatedSpace/Math.pow(1024,3);
			}
			outputds.append(dsName).append("\t").append(Math.round(allocatedSpace)).append("\t").append(Math.round(committed)).append("\n");
			output.append(outputds);
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
