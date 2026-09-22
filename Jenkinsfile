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
        stage('Check Workspace') {
    steps {
        sh '''
            pwd
            echo "===== Files ====="
            ls -la
            echo "===== Find pom.xml ====="
            find . -name pom.xml -type f
        '''
          }
       }        
    }
}