library(httr)
myToken <- "7a886..............................aa"
scriptFile <- 'myFoglightGroovyScript.groovy'

url="http://myfoglightserverurl"


login <- POST(url=paste(url,"/api/v1/security/login",sep=""), body=list(authToken=myToken), encode="form")

token <- content(login)$data$token

script <- readChar(scriptFile, file.info(scriptFile)$size)

req <- POST(url=paste(url,"/api/v1/script/runScript",sep=""), body=paste("<ScriptBean><script><![CDATA[", script, "]]></script></ScriptBean>"), encode="form", add_headers("Auth-Token" = token, "Accept" = "application/json", "Content-Type" = "application/xml"))

result <- content(req)$data$data

foglightdata <- read.csv(textConnection(result), header = TRUE, sep = "\t")

foglightdata
