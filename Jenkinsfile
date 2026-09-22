pipeline {
    agent {
        label 'Jenkins Server Agent-1'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                dir('sources/MavenProjects/LoanSettlementMaven') {
                    sh 'mvn clean package'
                }
            }
        }
    }
}