import groovy.json.JsonSlurperClassic 
import groovy.json.JsonOutput
import groovy.json.JsonBuilder

def map = [:]   // 제일먼저 선언
pipeline {
    agent any  
    tools {
        maven "jenkins-maven"
    } 
    environment{ 
        JIRA_CLOUD_CREDENTIALS = credentials('jira-cloud')  // Jenkins Web에서 설정한 값
        ISSUE_KEY = "${JIRA_TEST_PLAN_KEY}"                 // Jira trigger를 통해 자동으로 받는 값
        APPIUM_ADDR = "0.0.0.0"                             // stage('Download testcases on slave') : Real device로 테스트하기 때문에 0.0.0.0 으로 실행 => APPIUM_PORT="4723"
        BUILD_ID = "${BUILD_ID}"                            // Jenkins에서 자동으로 만들어줌
        BUILD_URL = "${BUILD_URL}"                          // Jenkins에서 자동으로 만들어줌

    }

    stages {
        stage('Init') {
            steps {
                script {
                    println "!!!!!!!!!!!!!!!!! Init !!!!!!!!!!!!!!!!!" 
                    println BUILD_ID
                    println BUILD_URL
                    init(map)   // 함수 아래에 정의하여 사용 => map.jira = [:] 선언
                    map.jira.auth_user = '$JIRA_CLOUD_CREDENTIALS_USR:$JIRA_CLOUD_CREDENTIALS_PSW'  // 보안때문에 작은 따옴표로 {} 없이 사용
                    map.jira.auth = "Basic " + "${JIRA_CLOUD_CREDENTIALS_USR}:${JIRA_CLOUD_CREDENTIALS_PSW}".bytes.encodeBase64()
                }
            }
        }
        // Test plan issue 가져오기 => Jira Pipeline steps 플러그인 설치
        stage('Get test plan'){
            steps{
                script{                   
                    println "!!!!!!!!!!!!!!!!! Get test plan !!!!!!!!!!!!!!!!!"
                    // ! Jira Pipeline steps 플러그인 => jiraGetIssue API 사용
                    map.issue = getJiraIssue(map.jira.base_url, map.jira.auth, ISSUE_KEY)
                    // println "Iseeue = > ${map.issue}"
                    map.jira.featureName = map.issue.fields.components[0].name
                    // Jira Component Field 아니면 에러
                    if(map.jira.featureName == null){
                        jenkinsException(map, "Jira Component Field is required")
                    }
                }
            }
        }
        // * 가져온 test plan에서 정보 습득하여 어떤 node에서 수행하는지 설정 
        // => Jira Cloud의 test plan의 이슈에 Tablet info 라는 커스텀 필드((셀렉트타입))를 추가 : 현재 호스트에 붙어있는 패드이름
        // Tablet info 필드: 필수입력값 => Field Configuration으로 설정, Renderers 타입을 Wiki로 
        stage('Get testcases / Set node'){
            steps{
                script{
                    println "!!!!!!!!!!!!!!!!! Get testcases / Set node !!!!!!!!!!!!!!!!!"
                    def jql = map.issue.fields[map.jira.testCaseJQLField]         
                    def testTablet = map.issue.fields[map.jira.tabletInfoField].value
                    // init method에서 지정해놓은 agents_ref 중 현재 설정된 Tablet info 필드 값과 일치하는 값이 있는지 확인 후 path, slave 설정
                    map.agents_ref.each { key, value -> 
                        if(testTablet == key){
                            map.current_node = key 
                            map.current_path = value
                        }
                    }
                    // 들 증 하나라도 다르면 에러
                    if(map.current_node == null || map.current_path == null){
                        jenkinsException(map, "Tablet Info Field is Required")
                    }
                    // jql로 이슈 가져오기
                    def result = getJiraIssuesByJql(map.jira.base_url, map.jira.auth, jql)
                    if(result.issues.size() == 0){      // 가져온 이슈 없으면 에러
                        jenkinsException(map, "Invalid JQL")
                    }
                    // issueKey:scenario => map에 저장
                    for (def issue in result.issues){
                        map.testcases.put(issue.key, issue.fields[map.jira.scenario_field].content[0].content[0].text)
                    }
                    println map.testcases

                }
            }
        }
        // 여기부터 pipeline 수행 
        stage('Download testcases on slave'){
            agent { label "${map.current_node}" }       // 지정한 slave node의 label
            steps {
                // dir로 path 지정 : 위에서 만들어둔 map.current_path 사용 => 이곳에서 slave작업 수행
                dir("${map.current_path}/workspace/yuna") {
                    script {
                        println "!!!!!!!!!!!!!!!!! Download testcases on slave !!!!!!!!!!!!!!!!!"
                        // feature Name 지정
                        // map.testcase에 담긴 각 시나리오를 feature 파일로 변환
                        def feature = "Feature: ${map.jira.featureName}\n\n"
                        //  JIRA에 올라가 있는 scenario를 가져와서 description으로 해당 JIRA issue key를 붙여준다.
                        //  issue key를 붙여주는 이유는 해당 시나리오가 JIRA에 어떤 issue와 매핑되는지 알기 위함
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
                            // fileExists : jenkins에서 제공하는 method
                        if (fileExists("${map.cucumber.feature_path}")){
                            bat script: """ rmdir /s /q "${map.cucumber.feature_path}" """, returnStdout:false
                        }   // 폴더 없으면 만들기
                         bat script: """ mkdir "${map.cucumber.feature_path}" """, returnStdout:false
                         // auto.feature 이름 파일 만들고 그 안에 jira에서 가져온 시나리오 다 적어 주기
                         writeFile(file: "./${map.cucumber.feature_path}/auto.feature", text:feature, encoding:"UTF-8")
                    }
                }
            }
        }
        // 테스트 실제 수행 위해 테스트 스크립트 빌드
        stage('Build'){
            agent { label "${map.current_node}" }
            steps {
                dir("${map.current_path}/workspace/yuna") {
                    script {
                        println "!!!!!!!!!!!!!!!!! Build !!!!!!!!!!!!!!!!!"
                        try {   // maven build project
                            bat script: 'mvn clean compile -D file.encoding=UTF-8 -D project.build.sourceEncoding=UTF-8 -D project.reporting.outputEncoding=UTF-8', returnStdout:false
                        } catch(error) {
                            throwableException(map, error)
                        }    
                    } 
                }
            }
        }
        // 테스트 수행 => appium server 실행 
        stage('Run automation testing'){
            // agent : 등록한 slavve에서만 해당 stage 실행
            agent { label "${map.current_node}" }
            steps {
                dir("${map.current_path}/workspace/yuna") {
                    script {
                        println "!!!!!!!!!!!!!!!!! Run automation testing !!!!!!!!!!!!!!!!!"
                        try {
                            // appium 연결/시작
                            // * returnStdout: true 옵션을 사용하면 해당 명령어의 실행 결과를 문자열로 반환. 기본값은 false이며, 이 경우 실행 결과를 콘솔 출력으로 표시
                            bat script: 'adb devices', returnStdout:false
                            // * 
                            // def devices = bat(returnStdout: true, script: 'adb devices')
                            // def serialNumbers = devices.split('\n')[1..-2].collect { it.split()[0] }
                            // echo "Serial numbers: ${serialNumbers}"
                            // def serialNumber = serialNumbers[2]
                            // echo "Serial number: ${serialNumber}"

                            // Background에서 실행 -> 다음 스테이지 실행하기 위해
                            bat "start /B appium --address ${APPIUM_ADDR} --port ${APPIUM_PORT}"
                            sleep 2

                            println "current node: " + map.current_node

                            // ! adb logcat 명령어를 실행.
                            if (map.current_node == "M2 Pad"){
                                def serialNumber = "WJD11AFN02513"
                                echo "Serial number: ${serialNumber}"
                                
                                def logcat_file = "logcat_${currentBuild.number}-${serialNumber}.txt"
                                bat "adb -s ${serialNumber} logcat -d > ${logcat_file}"
                                archiveArtifacts artifacts: logcat_file, fingerprint: true

//                                 bat "adb -s ${serialNumber} logcat -d > ${logcat_file}-${serialNumber}.txt"
//                                 // bat "adb -s ${serialNumber} logcat -d > ${logcat_file}.txt"
//                                 // bat "adb -s ${serialNumber} logcat -d | findstr \"EXCEPTION\" > failed_${logcat_file}.txt"
//                                 // archiveArtifacts artifacts: "logcat-${serialNumber}.txt, failed_${logcat_file}.txt", allowEmptyArchive: true
//                                 archiveArtifacts artifacts: "${logcat_file}-${serialNumber}.txt"
// }
                            }
                            if (map.current_node == "Others"){
                                def serialNumber = "WJD06AR00065"
                                echo "Serial number: ${serialNumber}"
                                bat "adb -s ${serialNumber} logcat -d > logcat-${serialNumber}.txt"
                                archiveArtifacts artifacts: "logcat-${serialNumber}.txt"
                            }
                            if (map.current_node == "T500"){
                                def serialNumber = "R9TT502BVQT"
                                echo "Serial number: ${serialNumber}"
                                bat "adb -s ${serialNumber} logcat -d > logcat-${serialNumber}.txt"
                                archiveArtifacts artifacts: "logcat-${serialNumber}.txt"
                            }
                            if (map.current_node == "T583"){
                                def serialNumber = "5200e1baf41648e7"
                                echo "Serial number: ${serialNumber}"
                                bat "adb -s ${serialNumber} logcat -d > logcat-${serialNumber}.txt"
                                archiveArtifacts artifacts: "logcat-${serialNumber}.txt"
                            }
                            // Windows에서 adb logcat 명령을 실행. -d 옵션을 사용하여 로그를 캡쳐하고 > 기호를 사용하여 logcat.txt 파일에 저장. 
                            // 마지막으로 readFile 명령어를 사용하여 파일을 읽고, echo 명령어를 사용하여 콘솔에 출력
                            // bat "adb logcat -d > ${map.current_path}/logcat.txt"
                            // def logcatContent = readFile "${map.current_path}/logcat.txt"
                            // echo logcatContent
                            // Extract logcat logs
                            // bat "adb logcat -d > logcat.txt"
                            //Publish logcat logs as build artifact
                            // archiveArtifacts artifacts: 'logcat.txt'
                             // 실패한 시나리오만 logcat 저장 하기
                            // bat script: 'adb logcat | grep "failed" > error.log'
                            // bat script: 'adb kill-server', returnStdout:false
                            // bat script: 'adb start-server', returnStdout:false

                             // ! 해당 파일 있으면 지우기 -> 할때마다 테스트 바뀌니까 (최신화)
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

                            // 테스트가 모두 끝나고 생성되는 cucumber.json 파일을 읽어서 map에 저장
                            // map.cucumber.result_text = readFile file: map.cucumber.report_json
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
                                        // Defect <> TestPlan 링크연결
                                        linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, ISSUE_KEY, "Defect"))
                                        // Defect <> Scenario 링크 연결
                                        linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, "${current_issue}", "Tests"))

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
                                        // Defect <> TestPlan 링크연결
                                        linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, ISSUE_KEY, "Defect"))
                                        // Defect <> Scenario 링크 연결
                                        linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, current_issue, "Tests"))
                                         
                                        continue 
                                    }
                                }
                                 
                                for(def step in r.steps) {
                                    if(!step.result.status.contains("passed")) {
                                        // TODO 로그 가져오기, 지라 defact issue 생성
                                        map.cucumber.errorMsg = step.result.error_message
                                        if(map.cucumber.errorMsg == null) {
                                            // TODO undefine인 경우 처리하기 (시나리오의 스텝이 있지만 메소드 구현이 안되어 있으면 undefine)
                                            map.cucumber.errorMsg = "${step.name} No Match Method"
                                            def bugPayload = createBugPayload("Defact of ${current_issue}", map.cucumber.errorMsg)
                                            def res = createJiraIssue(map.jira.base_url, map.jira.auth, bugPayload)
                                            // Defect <> TestPlan 링크연결
                                            linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, ISSUE_KEY, "Defect"))
                                            // Defect  <> Scenario 링크 연결
                                            linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, current_issue, "Tests"))     

                                            break 
                                        }
                                        isPassed = false
                                        def bugPayload = createBugPayload("Defect of ${current_issue}", map.cucumber.errorMsg)
                                        def res = createJiraIssue(map.jira.base_url, map.jira.auth, bugPayload)
                                        // Defect <> TestPlan 링크연결
                                        linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, ISSUE_KEY, "Defect"))
                                        // Defect <> Scenario 링크 연결
                                        linkIssue(map.jira.base_url, map.jira.auth, createLinkPayload(res.key, current_issue, "Tests"))
                                        // 디펙트인포를 가지고 정보를 전달
                                        map.cucumber.defect_info.put(res.key, scenario_name)
                                        
                                        break
                                    }                            
                                }
                            }

                            // if(isPassed){
                            //     // testplan 상태변경 (transition)
                            //     // ready >jenkins(postman) build> start >fail/success
                            //     transitionIssue(map.jira.base_url, map.jira.auth, transitionIssuePayload(map.jira.success_transition), ISSUE_KEY)
                            // } else {
                            //     // testplan 상태변경 (transition)
                            //     // ready >jenkins(postman) build> start >fail/success
                            //     transitionIssue(map.jira.base_url, map.jira.auth, transitionIssuePayload(map.jira.fail_transition), ISSUE_KEY)
                            // }
                            
                        } catch(error) {
                            throwableException(map, error)
                        }    
                    } 
                }
            }
        }
        // defect 이슈 화면 캡쳐
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
        // jenkins plugin : cucumber reports 설치
         stage('Generate cucumber reports'){
            agent { label "${map.current_node}" }
            steps {
                dir("${map.current_path}/workspace/yuna") {
                    script {
                        println "!!!!!!!!!!!!!!!!! Generate cucumber reports !!!!!!!!!!!!!!!!!"
                        try {
                            // https://plugins.jenkins.io/cucumber-reports/ 참고
                            cucumber buildStatus: 'UNSTABLE',
                                    reportTitle: 'cucumber report',
                                    fileIncludePattern: '**/*.json, **/logcat_*.txt',    // .json 으로된 모든 파일 => cucumber관련 없는 파일도 있을 수 있어서 명확한 파일 경로 설정해 주는것이 좋음
                                    trendsLimit: 10,
                                    classifications: [
                                        [
                                            'key': 'Browser',
                                            'value': 'Whale'   // 브라우저 우리가 쓰는걸로 변경
                                        ]
                                    ]

                            // cucumber reports 보고 주소 어떻게 변하는지 확인 후 아래 링크 설정
                            def reportLink = "${BUILD_URL}/${map.cucumber.report_link}"
                            
                            // cucumber reports 링크, build id 같이 올려줌
                            editIssue(map.jira.base_url, map.jira.auth,editIssuePayload(reportLink, BUILD_ID), ISSUE_KEY)

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
    map.jira.site_name = "REASONA"                      //! stage('Get test plan') / 내가 설정한 이름 
    map.jira.featureName = null
    map.jira.tabletInfoField = "customfield_10037"    
    map.jira.base_url = "https://reasona.atlassian.net" // stage('Get test plan') >> jira 주소  
    map.jira.testCaseJQLField ="customfield_10036"      // stage('Get testcases / Set node')
    map.testcases = [:]                                 // stage('Get testcases / Set node')
    map.jira.scenario_field = "customfield_10035"       // stage('Get testcases / Set node')
    map.jira.fail_transition = "21"                     // transition id : start -> test fail
    map.jira.success_transition = "31"                  // transition id : start -> test success
    // Jenkins에서 실행하는 workplace
    map.agents_ref = [
        "M2 Pad":"C:\\Users\\TB-NTB-223\\CICD\\m2"      // !stage('Get testcases / Set node') >> X500 : 호스트에 붙어있는 구동가능한 기계
    ]

    map.cucumber = [:]                                  // stage('Get test plan')
    map.cucumber.feature_path = "auto_features"         // stage('Download testcases on slave') : 파일 경로 생성
    map.cucumber.glue = "stepdefinitions"
    map.cucumber.report_json = "cucumber.json"          // stage('Run automation testing')
    map.cucumber.progress = "cucumber_progress.html"
    map.cucumber.cucumber_html = "cucumber_report.html"
    map.current_node = null                             // stage('Get testcases / Set node')
    map.current_path = null                             // stage('Get testcases / Set node')
    map.cucumber.result_json = null                     // stage('Run automation testing')
    map.cucumber.errorMsg = null
    map.cucumber.defect_info = [:]                      // defect 생길 경우, 해당 issueKey:scenario 명 저장
    map.cucumber.report_link = "cucumber-html-reports_f43d712d-34cf-37e2-891d-e19a85379e59/overview-features.html"  // !Jenkins pligin 설치 => cucumber build 시 job number 별로 html 파일 생성해줌
    map.issue = null    // stage('Get test plan') => 원하는 Test plan 이슈 가져 올 수 있음

}

// !jira cloud api 사용
def getJiraIssue (String baseURL, String auth, String issueKey){
    def conn = new URL("${baseURL}/rest/api/3/issue/${issueKey}").openConnection()
    conn.setRequestMethod("GET")
    conn.setDoOutput(true)
    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
    conn.addRequestProperty("Authorization", auth)
    def responseCode = conn.getResponseCode()
    def response = conn.getInputStream().getText()
    // 이슈정보 전달 위해서 response >> Json으로 변경해서 보내줌
    def result = new JsonSlurperClassic().parseText(response)

    if(responseCode != 200){
        throw new RuntimeException("Get Jira Issue Error -> " + conn.getErrorStream() +" response: "+ conn.getResponseMessage() +" code: "+ responseCode )
    }
    return result
}

// !JQL을 통해서 원하는 issue 가져오는 함수 
// jira cloud api 사용 : Issue search > Search for issues using JQL (GET)
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
                "name":"High"   // high, medium, low, lowest 중 선택
            ],
            "description" : [
               "type": "doc",
                "version": 1,
                "content": [
                    [ 
                        "type": "codeBlock",
                        "attrs": [
                            "language": "java"  // 사용하는 언어
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

// cucmber report, BUILD_ID 추가
def editIssuePayload(String reportLink, String buildlId) {
    def payload = [
        "fields":[
            "customfield_10038":"${reportLink}",
            "customfield_10039": "${buildlId}"
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
            "id":"${transition}"        // "2"
        ]
    ]

    return JsonOutput.toJson(payload)
}

def transitionIssue (String baseURL, String auth, String payload, String issueKey) {
    def conn = new URL("${baseURL}/rest/api/3/issue/${issueKey}/transitions").openConnection()
    conn.setRequestMethod("POST")
    conn.setDoOutput(true)
    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
    conn.addRequestProperty("Authorization", auth)
    if(payload) {
        conn.getOutputStream().write(payload.getBytes("UTF-8"))
    }
    def responseCode = conn.getResponseCode()
    def response = conn.getInputStream().getText()
   
    if(responseCode != 204){
        throw new RuntimeException("Transition Jira Issue Error -> " + conn.getErrorStream() +" response: "+ conn.getResponseMessage() +" code: "+ responseCode )
    }
}


