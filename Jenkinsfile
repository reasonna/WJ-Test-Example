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
        BUILD_ID = "${BUILD_ID}"    // Jenkins에서 자동으로 만들어줌
        BUILD_URL = "${BUILD_URL}"  // Jenkins에서 자동으로 만들어줌

    }

    stages {
        stage('Init') {
            steps {
                script {
                    println "!!!!!!!!!!!!!!!!! Init !!!!!!!!!!!!!!!!!" 
                    println BUILD_ID
                    println BUILD_URL
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
                        println "!!!!!!!!!!!!!!!!! Download testcases on slave !!!!!!!!!!!!!!!!!"
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
                            // 해당 폴더 있으면 지우기 -> 할때마다 테스트 바뀌니까 (최신화)
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
                        println "!!!!!!!!!!!!!!!!! Build !!!!!!!!!!!!!!!!!"
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
                        println "!!!!!!!!!!!!!!!!! Run automation testing !!!!!!!!!!!!!!!!!"
                        try {
                            // appium 연결/시작
                            bat script: 'adb devices', returnStdout:false
                            // bat script: 'adb kill-server', returnStdout:false
                            // bat script: 'adb start-server', returnStdout:false
                            // Background에서 실행 -> 다음 스테이지 실행하기 위해
                            bat "start /B appium --address ${APPIUM_ADDR} --port ${APPIUM_PORT}"
                            sleep 2
                             // 해당 파일 있으면 지우기 -> 할때마다 테스트 바뀌니까 (최신화)
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
        stage('Analysis test result'){
            agent { label "${map.current_node}" }
            steps {
                dir("${map.current_path}/workspace/yuna") {
                    script {
                        println "!!!!!!!!!!!!!!!!! Analysis test result !!!!!!!!!!!!!!!!!"
                        try {
                            map.cucumber.result_json = readFile file:map.cucumber.report_json
                            def report_json = new JsonSlurperClassic().parseText(map.cucumber.result_json as String)

                            def report_arr = report_json[0].elements
                            
                            def current_issue = null
                            def scenario_name = null

                            for(def r in report_arr){
                                current_issue = r.description.trim()
                                scenario_name = r.name.trim().replaceAll(" ", "_")

                                def before = r.before
                                def after = r.after
                                
                                if(before) {
                                   if(!before[0].result.status.contains("passed")) {
                                        // TODO 로그 가져오기, 지라 defact issue 생성
                                        map.cucumber.errorMsg = before[0].result.error_message
                                        def bugPayload = createBugPayload("Defact of ${current_issue}", map.cucumber.errorMsg)
                                        def res = createJiraIssue(map.jira.base_url, map.jira.auth, bugPayload)
                                        // Defect <> 링크연결
                                        linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, ISSUE_KEY, "Defect"))
                                        // Tests <> 링크 연결
                                        linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, ISSUE_KEY, "Tests"))

                                        map.cucumber.defect_info.put(res.key, scenario_name)

                                        
                                        continue 
                                   }
                                }
                                if(after) {
                                    if(!after[0].result.status.contains("passed")) {
                                        // TODO 로그 가져오기, 지라 defact issue 생성
                                        map.cucumber.errorMsg = after[0].result.error_message
                                        def bugPayload = createBugPayload("Defact of ${current_issue}", map.cucumber.errorMsg)
                                        def res = createJiraIssue(map.jira.base_url, map.jira.auth, bugPayload)

                                        linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, ISSUE_KEY, "Defect"))
                                        linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, ISSUE_KEY, "Tests"))


                                        continue 
                                    }
                                }
                                 
                                for(def step in r.steps) {
                                    if(!step.result.status.contains("passed")) {
                                        // TODO 로그 가져오기, 지라 defact issue 생성
                                        map.cucumber.errorMsg = step.result.error_message
                                        if(map.cucumber.errorMsg == null) {
                                            // TODO undefine인 경우 처리하기
                                            map.cucumber.errorMsg = "${step.name} No Match Method"
                                            def bugPayload = createBugPayload("Defact of ${current_issue}", map.cucumber.errorMsg)
                                            def res = createJiraIssue(map.jira.base_url, map.jira.auth, bugPayload)

                                            linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, ISSUE_KEY, "Defect"))
                                            linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, ISSUE_KEY, "Tests"))
                                            

                                            break 
                                        }
                                        def bugPayload = createBugPayload("Defect of ${current_issue}", map.cucumber.errorMsg)
                                        def res = createJiraIssue(map.jira.base_url, map.jira.auth, bugPayload)

                                        linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, ISSUE_KEY, "Defect"))
                                        linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, ISSUE_KEY, "Tests"))


                                        map.cucumber.defect_info.put(res.key, scenario_name)

                                        break
                                    }
                                }
                            }
                            

                        } catch(error) {
                            throwableException(map, error)
                        }    
                    } 
                }
            }
        }
        stage('Attach defect screenshot'){
            agent { label "${map.current_node}" }
            steps {
                dir("${map.current_path}/workspace/yuna") {
                    script {
                        println "!!!!!!!!!!!!!!!!! Attach defect screenshot !!!!!!!!!!!!!!!!!"
                        try {
                            if(map.cucumber.defect_info.size() > 0) {
                                map.cucumber.defect_info.each{key, value -> 
                                    bat encoding:"UTF-8", script: """curl --insecure -D- -u ${JIRA_CLOUD_CREDENTIALS_USR}:${JIRA_CLOUD_CREDENTIALS_PSW} -X POST -H "X-Atlassian-Token: no-check" -F "file=@./defect_screenshots/${value}.png" ${map.jira.base_url}/rest/api/3/issue/${key}/attachments""", returnStdout:false
                                }
                            }
                            
                        } catch(error) {
                            throwableException(map, error)
                        }    
                    } 
                }
            }
        }
         stage('Generate cucumber reports'){
            agent { label "${map.current_node}" }
            steps {
                dir("${map.current_path}/workspace/yuna") {
                    script {
                        println "!!!!!!!!!!!!!!!!! Generate cucumber reports !!!!!!!!!!!!!!!!!"
                        try {
                            cucumber buildStatus: 'UNSTABLE',
                                    reportTitle: 'cucumber report',
                                    fileIncludePattern: '**/*.json',    // .json 으로된 모든 파일 => cucumber관련 없는 파일도 있을 수 있어서 명확한 파일 경로 설정해 주는것이 좋음
                                    trendsLimit: 10,
                                    classifications: [
                                        [
                                            'key': 'Browser',
                                            'value': 'Chrome'
                                        ]
                                    ]

                            def reportLink = "${BUILD_URL}/${map.cucumber.report_link}"
                            
                            editIssue(map.jira.base_url, map.jira.auth,editIssuePayload(reportLink), ISSUE_KEY)

                        } catch(error) {
                            throwableException(map, error)
                        }    
                    } 
                }
            }
        }

        stage('Transition Issue'){
            agent { label "${map.current_node}" }
            steps {
                dir("${map.current_path}/workspace/yuna") {
                    script {
                        println "!!!!!!!!!!!!!!!!! Transition Issue !!!!!!!!!!!!!!!!!"
                        try {
                            def transition = null
                            transitionIssue(map.jira.base_url, map.jira.auth, transitionIssuePayload("${transition}"))
                            
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
    map.cucumber.feature_path = "auto_features"         // 파일 경로 생성
    map.cucumber.glue = "stepdefinitions"
    map.cucumber.report_json = "cucumber.json"
    map.cucumber.progress = "cucumber_progress.html"
    map.cucumber.cucumber_html = "cucumber_report.html"
    map.current_node = null
    map.current_path = null
    map.cucumber.result_json = null
    map.cucumber.errorMsg = null
    map.cucumber.defect_info = [:]                      // defect 생길 경우, 해당 issueKey:scenario 명 저장
    map.cucumber.report_link = "cucumber-html-reports_f43d712d-34cf-37e2-891d-e19a85379e59/overview-features.html"  // Jenkins pligin 설치 => cucumber build 시 job number 별로 html 파일 생성해줌
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
    //  json으로 바꿔서 값 받기
    def result = new JsonSlurperClassic().parseText(response)
    
    if(responseCode != 200){
        throw new RuntimeException("Get Jira Issue Error -> " + conn.getErrorStream() +" response: "+ conn.getResponseMessage() +" code: "+ responseCode )
    }
    println result
    return result
}

def createBugPayload(String summary, String errorMessage) {
    def payload = [
         "fields": [
            "summary": "${summary}",
            "project": [
                "id": "10002"
            ],
            "issuetype": [
                "id": "10006"
            ],
            "priority":[
                "name":"High"
            ],
            "description" : [
               "type": "doc",
                "version": 1,
                "content": [
                    [ 
                        "type": "codeBlock",
                        "attrs": [
                            "language": "java"
                            ],
                        "content": [
                            [
                            "text":" ${errorMessage}",
                            "type": "text"
                            ]
                        ]
                    ]
                ]
            ],
            "assignee": [
                "id": "63bceb9d713349bea186f1f5"
            ]
         ]
    ]
    return JsonOutput.toJson(payload)
}

def createJiraIssue (String baseURL, String auth, String bugPayload) {
    def conn = new URL("${baseURL}/rest/api/3/issue").openConnection()
    conn.setRequestMethod("POST")
    conn.setDoOutput(true)
    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
    conn.addRequestProperty("Authorization", auth)
    if(bugPayload) {
        conn.getOutputStream().write(bugPayload.getBytes("UTF-8"))
    }
    def responseCode = conn.getResponseCode()
    def response = conn.getInputStream().getText()
    def result = new JsonSlurperClassic().parseText(response)
    if(responseCode != 201){
        throw new RuntimeException("Create Jira Issue Error -> " + conn.getErrorStream() +" response: "+ conn.getResponseMessage() +" code: "+ responseCode )
    }
    return result
}

def createLinkPayload(String outwardIssue, String inwardIssue, String linkType) {
    def payload = [
        "outwardIssue": [
            "key": "${outwardIssue}"
        ],
            "inwardIssue": [
            "key": "${inwardIssue}"
            ],
        "type": [
            "name": "${linkType}"
        ]
    ]
    return JsonOutput.toJson(payload)
}

def linkIssue (String baseURL, String auth, String payload) {
    def conn = new URL("${baseURL}/rest/api/3/issueLink").openConnection()
    conn.setRequestMethod("POST")
    conn.setDoOutput(true)
    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
    conn.addRequestProperty("Authorization", auth)
    if(payload) {
        conn.getOutputStream().write(payload.getBytes("UTF-8"))
    }
    def responseCode = conn.getResponseCode()
    def response = conn.getInputStream().getText()
    println response
    if(responseCode != 201){
        throw new RuntimeException("Link Jira Issue Error -> " + conn.getErrorStream() +" response: "+ conn.getResponseMessage() +" code: "+ responseCode )
    }
}

// cucmber report 추가
def editIssuePayload(String reportLink) {
    def payload = [
        "fields":[
            "customfield_10038":"${reportLink}"
        ]
    ]
    return JsonOutput.toJson(payload)
}

def editIssue (String baseURL, String auth, String payload, String issueKey) {
    def conn = new URL("${baseURL}/rest/api/3/issue/${issueKey}").openConnection()
    conn.setRequestMethod("PUT")
    conn.setDoOutput(true)
    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
    conn.addRequestProperty("Authorization", auth)
    if(payload) {
        conn.getOutputStream().write(payload.getBytes("UTF-8"))
    }
    def responseCode = conn.getResponseCode()
    def response = conn.getInputStream().getText()
   
    if(responseCode != 204){
        throw new RuntimeException("Edit Jira Issue Error -> " + conn.getErrorStream() +" response: "+ conn.getResponseMessage() +" code: "+ responseCode )
    }
}

def transitionIssuePayload(String transition) {
    def payload = [
        "transition":[
            "id":"${transition}"
        ]
    ]
    return JsonOutput.toJson(payload)
}

def transitionIssue (String baseURL, String auth, String payload, String issueKey) {
    def conn = new URL("${baseURL}/rest/api/3/issue/${issueKey}/transitions").openConnection()
    conn.setRequestMethod("PUT")
    conn.setDoOutput(true)
    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
    conn.addRequestProperty("Authorization", auth)
    if(payload) {
        conn.getOutputStream().write(payload.getBytes("UTF-8"))
    }
    def responseCode = conn.getResponseCode()
    def response = conn.getInputStream().getText()
   
    if(responseCode != 204){
        throw new RuntimeException("Edit Jira Issue Error -> " + conn.getErrorStream() +" response: "+ conn.getResponseMessage() +" code: "+ responseCode )
    }
}