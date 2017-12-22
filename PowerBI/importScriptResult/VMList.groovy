import java.text.SimpleDateFormat;
import com.quest.nitro.service.sl.interfaces.data.ObservationQuery.RetrievalType;

ts = server.TopologyService;
ds = server.DataService;

def countLimit=50
def logException = false
org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog("Q1.groovy");

currentDate = new Date();

endTime = currentDate.getTime()

Calendar c = Calendar.getInstance();
c.setTime(currentDate);
c.add(Calendar.DAY_OF_MONTH, -1);

startTime = c.getTime().getTime();

vmType = ts.getType("VMWVirtualMachine");

vmSet = ts.getObjectsOfType(vmType)

def output = new StringBuilder();

output.append("Start Time").append(",").append("End Time").append(",").append("vCenter Name").append(",").append("cluster name").append(",").append("esxhost ").append(",").append("vm name").append(",").append("VM CPU allocated(MHz)").append(",").append("VM CPU used(MHz)").append(",").append("VM memory allocated(GB)").append(",").append("VM memory used(GB)").append(",").append("datastore name").append(",").append("VM ds allocated(GB)").append(",").append("VM ds used(GB)").append("\n")


metricsToObjects = getVmMetricsToObjMap();
def result = batchQueryVmMetrics();



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
		
		vmCpus = vm?.get("cpus/hostCPUs");		
		vmMemory = vm?.get("memory");
		if(vmCpus == null || vmMemory == null){
			continue
		}
		
		dsUsageList = vm.get("storage/datastoreUsage");

		
		cpuUsedValues = result.getValues(vm, "usedHz");
		cpuUtilizationValues = result.getValues(vmCpus, "utilization");
		
		memoryAllocatedValues = result.getValues(vmMemory, "allocated");
		memoryUsed = result.getValues(vmMemory, "consumed");
		
		for (int i = 0; i < 23; i++) {
		    def start = cpuUsedValues.get(i).getStartTime();
			def end = cpuUsedValues.get(i).getEndTime()
		
		    vmCPUUsed = cpuUsedValues.get(i)?.getValue()?.getAvg();
			vmCPUUtilization = cpuUtilizationValues.get(i)?.getValue()?.getAvg();
			def vmCpuAllocated;
			
			if (vmCPUUsed != null && !vmCPUUsed.isNaN()) {
			    vmCPUUsed =  new BigDecimal(vmCPUUsed/Math.pow(1000,2)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();		
			    vmCpuAllocated = new BigDecimal(vmCPUUsed*100/vmCPUUtilization).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
		    }
			
			vmMemAllocated = memoryAllocatedValues.get(i)?.getValue()?.getAvg();
			vmMemUsed = memoryUsed.get(i)?.getValue()?.getAvg();
			
			if (vmMemAllocated != null && !vmMemAllocated.isNaN()) {
			    vmMemAllocated = new BigDecimal(vmMemAllocated/1024).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
		    }
			
		    if (vmMemUsed != null && !vmMemUsed.isNaN()) {
			    vmMemUsed = new BigDecimal(vmMemUsed/Math.pow(1024,2)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
		    }
			def outputMid = new StringBuilder();
			outputMid.append(start).append(",").append(end).append(",").append(vcName).append(",").append(clusterName).append(",").append(esxName).append(",").append(vmName).append(",").append(vmCpuAllocated).append(",").append(vmCPUUsed).append(",").append(vmMemAllocated).append(",").append(vmMemUsed).append(",");
            
					
		    for (dsUsage in dsUsageList) {
		        def outputds = new StringBuilder(outputMid.toString());
				dsName = dsUsage.datastore?.name;
				committed = result.getValues(dsUsage, "committed").get(i)?.getValue()?.getAvg();
				uncommitted = result.getValues(dsUsage, "uncommitted").get(i)?.getValue()?.getAvg();
				allocatedSpace  = committed + uncommitted; 
			    if (committed != null && !committed.isNaN()) {
				    committed = committed/Math.pow(1024,3);
			    }
			    if (allocatedSpace != null && !allocatedSpace.isNaN()) {
				    allocatedSpace = allocatedSpace/Math.pow(1024,3);
			    }
			    outputds.append(dsName).append(",").append(Math.round(allocatedSpace)).append(",").append(Math.round(committed)).append("\n");
			    output.append(outputds);
		    }
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

def addObjToSet(obj, objSet) {
    if (obj) {
	    objSet.add(obj);
	}
}

def getVmMetricsToObjMap() {
    def metricsToObjects = [:];
	def vmCpuSet = [] as Set;
	def vmMemSet = [] as Set;
	def dsUsageSet = [] as Set;
    try{        
	    metricsToObjects.put("usedHz", vmSet);
        for (vm in vmSet) {	    
            isTemplate = vm.isTemplate;
		    if(isTemplate) {
			    continue;
		    }	
            addObjToSet(vm?.get("cpus/hostCPUs") ,vmCpuSet);			
            addObjToSet(vm?.get("memory") ,vmMemSet);
			datastoreUsage = vm?.get("storage/datastoreUsage");
            for (dsUsage in datastoreUsage){
			    addObjToSet(dsUsage, dsUsageSet);
            }	
        }
		metricsToObjects.put("utilization", vmCpuSet);
		metricsToObjects.put("allocated", vmMemSet);
		metricsToObjects.put("consumed", vmMemSet);
		metricsToObjects.put("committed", dsUsageSet);
		metricsToObjects.put("uncommitted", dsUsageSet);		
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