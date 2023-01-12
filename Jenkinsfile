import groovy.json.JsonSlurperClassic 
import groovy.json.JsonOutput
import groovy.json.JsonBuilder

def map = [:]
pipeline {
    agent any   
    environment{ 
        JIRA_CLOUD_CREDENTIALS = credentials('jira-cloud')
        ISSUE_KEY = "${JIRA_TEST_PLAN_KEY}"

    }

    stages {
        stage('Init') {
            steps {
                script {
                    println "!!!!!!!!!!!!!!!!! Init !!!!!!!!!!!!!!!!!" 
                    init(map)   
                    map.jira.auth_user = '$JIRA_CLOUD_CREDENTIALS_USR:$JIRA_CLOUD_CREDENTIALS_PSW'  
                    map.jira.auth = "Basic " + "${JIRA_CLOUD_CREDENTIALS_USR}:${JIRA_CLOUD_CREDENTIALS_PSW}".bytes.encodeBase64()
                }
            }
        }
        stage('Get test plan'){
            steps{
                script{                   
                    println "!!!!!!!!!!!!!!!!! Get test plan !!!!!!!!!!!!!!!!!"
                    map.issue = getJiraIssue(map.jira.base_url, map.jira.auth, ISSUE_KEY)
                    // println "Iseeue = > ${map.issue}"
                    map.jira.featureName = map.issue.fields.components[0].name
                    if(map.jira.featureName == null){
                        jenkinsException(map, "Jira Component Field is required")
                    }
                }
            }
        }
        stage('Get testcases / Set node'){
            steps{
                script{
                    println "!!!!!!!!!!!!!!!!! Get testcases / Set node !!!!!!!!!!!!!!!!!"
                    def jql = map.issue.fields[map.jira.testCaseJQLField]         
                    def testTablet = map.issue.fields[map.jira.tabletInfoField].value
                    map.agents_ref.each { key, value -> 
                        if(testTablet == key){
                            map.current_node = key 
                            map.current_path = value
                        }
                    }
                    if(map.current_node == null || map.current_path == null){
                        jenkinsException(map, "Tablet Info Field is Required")
                    }

                    def result = getJiraIssuesByJql(map.jira.base_url, map.jira.auth, jql)
                    if(result.issues.size() == 0){
                        jenkinsException(map, "Invalid JQL")
                    }
                    for (def issue in result.issues){
                        map.testcases.put(issue.key, issue.fields[map.jira.scenario_field].content[0].content[0].text)
                    }
                    println map.testcases

                }
            }
        }
        stage('Download testcases on slave'){
            agent { label "${map.current_node}" }
            steps {
                dir("${map.current_path}") {
                    script {
                        def feature = "Feature: ${map.jira.featureName}"
                        map.testcases.each { key, value ->
                            def addDescription = null 
                            if (value.contains("\r\n")){
                                addDescription = value.replaceFirst("\r\n", ("\r\n" + key + "\n\n"))
                                feature += addDescription 
                                feature += "\n\n" 
                            } else {
                                addDescription = value.replaceFirst("\n", ("\n"+ key + "\n\n"))
                                feature += addDescription 
                                feature += "\n\n" 
                            }
                        }
                        if (fileExists("${map.cucumber.feature_path}")){
                            bat script: """ rmdir /s /q "${map.cucumber.feature_path}" """, returnStdout:false
                        }
                         bat script: """ mkdir "${map.cucumber.feature_path}" """, returnStdout:false
                         writeFile(file: "./${map.cucumber.feature_path}/auto.feature", text:feature, encoding:"UTF-8")
                    }
                }
            }
        }
        
    }
}

def jenkinsException(java.util.Map map, String error){
    map.exceptionMsg = error
    throw new RuntimeException("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + error + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
}
def init (def map){
    map.jira = [:]
    map.jira.site_name = "REASONA"
    map.jira.base_url = "https://reasona.atlassian.net"
    map.jira.featureName = null
    map.jira.tabletInfoField = "customfield_10037"
    map.jira.testCaseJQLField ="customfield_10036"
    map.testcases = [:]
    map.jira.scenario_field = "customfield_10035"
    map.agents_ref = [
        "X500":"C:\\Users\\TB-NTB-223\\CICD\\X500"      //구동가능한 기계
    ]
    map.cucumber = [:]
    map.cucumber.feature_path = "auto_features"
    map.current_node = null
    map.current_path = null

    map.issue = null

}

def getJiraIssue (String baseURL, String auth, String issueKey){
    def conn = new URL("${baseURL}/rest/api/3/issue/${issueKey}").openConnection()
    conn.setRequestMethod("GET")
    conn.setDoOutput(true)
    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
    conn.addRequestProperty("Authorization", auth)
    def responseCode = conn.getResponseCode()
    def response = conn.getInputStream().getText()
    def result = new JsonSlurperClassic().parseText(response)

    if(responseCode != 200){
        throw new RuntimeException("Get Jira Issue Error -> " + conn.getErrorStream() +" response: "+ conn.getResponseMessage() +" code: "+ responseCode )
    }
    return result
}

def getJiraIssuesByJql (String baseURL, String auth, String jql){
    def jqlEncode = java.net.URLEncoder.encode(jql, "UTF-8")
    def conn = new URL("${baseURL}/rest/api/3/search?jql=${jqlEncode}").openConnection()
    conn.setRequestMethod("GET")
    conn.setDoOutput(true)
    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
    conn.addRequestProperty("Authorization", auth)
    def responseCode = conn.getResponseCode()
    def response = conn.getInputStream().getText()
    def result = new JsonSlurperClassic().parseText(response)

    if(responseCode != 200){
        throw new RuntimeException("Get Jira Issue Error -> " + conn.getErrorStream() +" response: "+ conn.getResponseMessage() +" code: "+ responseCode )
    }
    println result
    return result
}
