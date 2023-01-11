import groovy.json.JsonSlurper 
import groovy.json.JsonOutput
import groovy.json.JsonBuilder

def map = [:]
pipeline {
        agent any
        environment{
            JIRA_CLOUD_CREDENTIALS = credentials('jira-cloud')
        }

        stages {
            stage('Init') {
                steps {
                    script {
                        println "!!!!!!!!!!!!! Init !!!!!!!!!!!!!!"
                        init(map)
                        map.jira.auth_user = '$JIRA_CLOUD_CREDENTIALS_USR:$JIRA_CLOUD_CREDENTIALS_PSW'
                        map.jira.auth = "Basic " + "${JIRA_CLOUD_CREDENTIALS_USR}:${JIRA_CLOUD_CREDENTIALS_PSW}".bytes.encodeBase64()
                }
            }
        }
            stage('Get test plan'){
                steps{
                    script{
                        println "!!!!!!!!!!!!! Get test plan !!!!!!!!!!!!!!!!!"
                        getJiraIssue(map.jira.base_url, map.jira.auth, "WC-3")
                        // println "Iseeue = > ${map.issue}"
                    }
                }
            }
    }
}

def init (def map){
    map.jira = [:]
    map.jira.site_name = "REASONA"
    map.jira.base_url = "https://reasona.atlassian.net"

    map.issue = null
}

def getJiraIssue (String baseURL, String auth, String issueKey){
    def conn = new URL("${baseURL}/rest/api/3/issue/${issueKey}").openConnection()
    conn.setReuestMethod("GET")
    conn.setDoOutput(true)
    conn.setRequestProperty("Content-Type", "Appilcation/json")
    conn.setRequestProperty("Authorization", auth)
    def responseCode = conn.getResponseCode()
    def response = conn.getInputStream().getText()
    def result = new JsonSlurper().parseText(response)

    println responseCode
    println response
    println result
}
