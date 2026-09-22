pipeline {
    agent {
        node {
            label 'Jenkins Server Agent-1'
        }
    }

    stages {
        stage('Step 1: Checkout Source Code') {
            steps {
                checkout scm
            }
        }

        stage('Step 2: Check TAFJ & T24 Lib Paths on Agent') {
            steps {
                sh 'mvn clean package'
            }
        }
    }
}