import groovy.json.JsonSlurperClassic 
import groovy.json.JsonOutput
import groovy.json.JsonBuilder

def map = [:]
pipeline {
    agent any  
    tools {
        maven "jenkins-maven"
    } 
    environment{ 
        JIRA_CLOUD_CREDENTIALS = credentials('jira-cloud')
        ISSUE_KEY = "${JIRA_TEST_PLAN_KEY}"
        APPIUM_ADDR = "0.0.0.0"
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
                // dir로 path 지정 => 이곳에서 slave작업 수행
                dir("${map.current_path}/workspace/yuna") {
                    script {
                        def feature = "Feature: ${map.jira.featureName}\n\n"
                        map.testcases.each { key, value ->
                            def addDescription = null 
                            if (value.contains("\r\n")){
                                // 해당 jira issue key 붙여줌
                                addDescription = value.replaceFirst("\r\n", ("\r\n" + key + "\n\n"))
                                feature += addDescription 
                                feature += "\n\n" 
                            } else {
                                addDescription = value.replaceFirst("\n", ("\n"+ key + "\n\n"))
                                feature += addDescription 
                                feature += "\n\n" 
                            }
                        }
                            // 해당 파일 있으면 지우기 -> 할때마다 테스트 바뀌니까 (최신화)
                        if (fileExists("${map.cucumber.feature_path}")){
                            bat script: """ rmdir /s /q "${map.cucumber.feature_path}" """, returnStdout:false
                        }   // 파일 없으면 만들기
                         bat script: """ mkdir "${map.cucumber.feature_path}" """, returnStdout:false
                         // auto.feature 이름 파일 만들고 그 안에 jira에서 가져온 시나리오 다 적어 주기
                         writeFile(file: "./${map.cucumber.feature_path}/auto.feature", text:feature, encoding:"UTF-8")
                    }
                }
            }
        }
        // 테스트 스크립트 빌드
        stage('Build'){
            agent { label "${map.current_node}" }
            steps {
                dir("${map.current_path}/workspace/yuna") {
                    script {
                        try {
                            bat script: 'mvn clean compile -D file.encoding=UTF-8 -D project.build.sourceEncoding=UTF-8 -D project.reporting.outputEncoding=UTF-8', returnStdout:false
                        } catch(error) {
                            throwableException(map, error)
                        }    
                    } 
                }
            }
        }
        // 테스트 수행 => appium
        stage('Run automation testing'){
            agent { label "${map.current_node}" }
            steps {
                dir("${map.current_path}/workspace/yuna") {
                    script {
                        try {
                            // appium 연결/시작
                            bat script: 'adb devices', returnStdout:false
                            // bat script: 'adb kill-server', returnStdout:false
                            // bat script: 'adb start-server', returnStdout:false
                            // Background에서 실행 -> 다음 스테이지 실행하기 위해
                            bat "start /B appium --address ${APPIUM_ADDR} --port ${APPIUM_PORT}"
                            sleep 2

                            if (fileExists("${map.cucumber.report_json}")){
                                bat script: """ del "${map.cucumber.report_json}" """, returnStdout:false
                            }
                            if (fileExists("${map.cucumber.progress}")){
                                bat script: """ del "${map.cucumber.progress}" """, returnStdout:false
                            }
                            if (fileExists("${map.cucumber.cucumber_html}")){
                                bat script: """ del "${map.cucumber.cucumber_html}" """, returnStdout:false
                            }

                            try{
                                bat encoding:"UTF-8", script: "mvn exec:java -Dproject.build.sourceEncoding=UTF-8 -Dproject.reporting.outputEncoding=UTF-8 -Dexec.mainClass=io.cucumber.core.cli.Main -Dexec.args=\"${map.cucumber.feature_path} --glue ${map.cucumber.glue} --plugin json:${map.cucumber.report_json} --plugin progress:${map.cucumber.progress} --publish --plugin pretty --plugin html:${map.cucumber.cucumber_html}\"", returnStdout:false
                            } catch(error){
                                println "Automation testing error -> " + error
                            }
                        } catch(error) {
                            throwableException(map, error)
                        }    
                    } 
                }
                dir("${map.current_path}/workspace/yuna") {
                    script {
                        try {
                            output = bat encoding: "UTF-8", script: "netstat -aon | findstr 0.0.0.0:${APPIUM_PORT} | findstr LISTENING", returnStdout:true
                            pid = output.substring(output.length()-9)
                            sleep 2 
                            println pid 
                            kill_output = bat encoding: "UTF-8", script: "taskkill /F /IM ${pid}", returnStdout:true
                            echo kill_output

                        } catch(error) {
                            throwableException(map, error)
                        }
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

def throwableException(java.util.Map map, Exception e) {
    map.exceptionMsg = e.toString()
    throw e as java.lang.Throwable 
}
def init (def map){
    map.jira = [:]
    map.jira.site_name = "REASONA"                      // 내가 설정한 이름
    map.jira.base_url = "https://reasona.atlassian.net" // jira 주소
    map.jira.featureName = null
    map.jira.tabletInfoField = "customfield_10037"      // json online viewer로 보기
    map.jira.testCaseJQLField ="customfield_10036"
    map.testcases = [:]
    map.jira.scenario_field = "customfield_10035"
    map.agents_ref = [
        "X500":"C:\\Users\\TB-NTB-223\\CICD\\X500"      //구동가능한 기계
    ]
    map.cucumber = [:]
    map.cucumber.feature_path = "auto_features"
    map.cucumber.glue = "stepdefinitions"
    map.cucumber.report_json = "cucumber.json"
    map.cucumber.progress = "cucumber_progress.html"
    map.cucumber.cucumber_html = "cucumber_report.html"
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
