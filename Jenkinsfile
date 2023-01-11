def map = [:]
pipeline {
    agent any
    environment{
        JIRA_CLOUD_CREDENTIANLS = credentials('jira-cloud')
    }

    stages {
        stage('Init') {
            steps {
                script {
                    println "!!!!!!!!!!!!! Init !!!!!!!!!!!!!!"
                    init(map)
                    map.jira.auth_user = '$JIRA_CLOUD_CREDENTIANLS_USR:$JIRA_CLOUD_CREDENTIANLS_PSW'
                    map.jira.auth = "Basic " + "${JIRA_CLOUD_CREDENTIANLS_USR}:${$JIRA_CLOUD_CREDENTIANLS_PSW}".bytes.encodeBase64()
            }
        }
    }
    stage('Get test plan'){
        steps{
            script{
                println "!!!!!!!!!!!!! Get test plan !!!!!!!!!!!!!!!!!"
                map.issue = jiraGetIssue idOrKey: 'WC-3', site: map.jira.site_name
                println "Iseeue = > ${map.issue}"
            }
        }
    }
}
}

def init (def map){
    map.jira = [:]
    map.jira.site_name = "REASONA_CLOUD"

    map.issue = null
}
