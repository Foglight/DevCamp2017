import java.util.Random;

def scriptSrv = server.ScriptingService;
def getTopologyBuilderScript = scriptSrv.getNamedScript("getTopologyBuilder");

if(getTopologyBuilderScript){
	osArchitecture = "ia32";
	osNameMap = ["Windows":"Microsoft(R) Windows(R) Server 2003 Standard ia32 Edition", "Linux":"CentOS 6", "AIX":"AIX", "HPUX":"HPUX", "Solaris":"Solaris"];
	osTypes = ["Windows", "Linux", "AIX", "HPUX", "Solaris"];
	submissionRate = 900000;
	def random = new Random();
	
	def hostnames = "db-1,db-2,db-3,db-4,app-1,app-2,app-3,app-4,app-5,app-6,app-7,app-8,lb-1,lb-2,lb-3,lb-4,web-1,web-2,web-3,web-4,web-5,web-6,web-7";


	def builder = getTopologyBuilder();
	def names = hostnames.split(', *') as List;
	names.collect {hostname ->
		def osType = osTypes.get(random.nextInt(5));
		def osName = osNameMap.get(osType);
		builder.Host(hostname) {

			_ agents : [Agent (agentID:999999999, agentName:'Data Submitter')];
			_ os : OperatingSystem() {
				_ type: osType;
				_ name: osName;
				_ architecture: osArchitecture;
			}
			_ cpus : HostCPUs() {
				_ processors : [
					Processor('0'),
				]
			};
			_ memory : Memory();
			_ storage : HostStorage () {
				_ logicalDisks : [
					LogicalDisk ('C:'),
				]
			};
			_ network : HostNetwork () {
				_ interfaces : [
					NetworkInterface () {
						_ name:'Interface 1';
						_ interfaceIndex:65539;
						_ interfaceType:'Ethernet 802.3';
						_ macAddress:'00:00:00:00:00:00';
					},
				]
			};
			_ name : hostname;
		}
	}
	def hosts = builder.mergeObjects().findAll {object ->
		return 'Host'.equals(object.getType().getName());
	};
	
	hosts.collect {host ->
		return submitHostMetric(host, submissionRate);
	}
}else{
	System.err.println("Generate simulate data fail due to some naming script is lost, please make sure 'Topology-Helper' car has install.");
}
