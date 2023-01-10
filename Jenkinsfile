def map = [:]
pipeline {
    agent any
    environment{
        JIRA_CLOUD_CREDENTIANLS = credentials("jira-cloud")
    }

    stages {
        stage('Init') {
            steps {
                script {
                    println ✅✅ Init  ✅✅
                    init(map)
                    map.jira.auth_user = '$JIRA_CLOUD_CREDENTIANLS_USR:$JIRA_CLOUD_CREDENTIANLS_PSW'
                    println map.jira.auth_user
                }
            }
        }
    }
}

def init (def map){
    map.jira = [:]
}
